package org.petmeet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.entity.OmsAfterSale;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.OmsAfterSaleMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.AdminOrderService;
import org.petmeet.service.PmsProductService;
import org.petmeet.service.SysNotificationService;
import org.petmeet.vo.AdminOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final OmsAfterSaleMapper afterSaleMapper;
    private final SysUserMapper userMapper;
    private final PmsProductService productService;
    private final SysNotificationService notificationService;

    /**
     * 订单列表
     */
    @Override
    public Page<AdminOrderVO> pageList(Integer pageNum, Integer pageSize, Integer status, String orderNo,
                                       String startTime, String endTime) {
        Page<OmsOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(OmsOrder::getAdminDeleted).or().eq(OmsOrder::getAdminDeleted, 0));

        if (status != null) {
            wrapper.eq(OmsOrder::getStatus, status);
        }
        if (StrUtil.isNotBlank(orderNo)) {
            String keyword = orderNo.trim();
            List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .select(SysUser::getId)
                            .and(w -> w.like(SysUser::getUsername, keyword)
                                    .or().like(SysUser::getNickname, keyword)
                                    .or().like(SysUser::getPhone, keyword)))
                    .stream()
                    .map(SysUser::getId)
                    .filter(Objects::nonNull)
                    .toList();

            // 支持按订单号或买家身份信息（用户名/昵称/手机号）搜索。
            wrapper.and(w -> {
                w.like(OmsOrder::getOrderSn, keyword);
                if (!matchedUserIds.isEmpty()) {
                    w.or().in(OmsOrder::getUserId, matchedUserIds);
                }
            });
        }
        if (StrUtil.isNotBlank(startTime)) {
            wrapper.ge(OmsOrder::getCreateTime, LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(OmsOrder::getCreateTime, LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        wrapper.orderByDesc(OmsOrder::getCreateTime);

        // 查询后台订单分页
        Page<OmsOrder> orderPage = orderMapper.selectPage(page, wrapper);
        // 转成后台展示对象
        Page<AdminOrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 订单详情
     */
    @Override
    public AdminOrderVO getDetail(Long id) {
        return toVO(requireOrder(id));
    }

    /**
     * 订单发货
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id, String company, String trackingNo) {
        // 查询订单并校验状态
        OmsOrder order = requireOrder(id);

        if (order.getStatus() == OmsOrder.STATUS_REFUNDING) {
            throw new RuntimeException("This order is refunding. Pause shipment and handle refund first");
        }
        if (order.getStatus() == null || order.getStatus() != OmsOrder.STATUS_PAID) {
            throw new RuntimeException("Only paid orders can be shipped");
        }

        Long activeAfterSale = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getOrderId, order.getId())
                .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING));
        if (activeAfterSale != null && activeAfterSale > 0) {
            throw new RuntimeException("This order has active after-sale requests. Pause shipment first");
        }

        // 更新发货信息
        order.setStatus(OmsOrder.STATUS_SHIPPED);
        order.setShipCompany(StrUtil.trimToNull(company));
        order.setTrackingNo(StrUtil.trimToNull(trackingNo));
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 通知用户订单已发货
        notificationService.sendToUser(
                order.getUserId(),
                "订单已发货",
                "你的订单" + order.getOrderSn() + "已发货",
                "order",
                order.getId()
        );
    }

    /**
     * 处理退款
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id, Map<String, Object> refundInfo) {
        // 查询退款中的订单
        OmsOrder order = requireOrder(id);
        if (order.getStatus() == null || order.getStatus() != OmsOrder.STATUS_REFUNDING) {
            throw new RuntimeException("Only refunding orders can be handled here");
        }

        boolean success = parseBoolean(refundInfo == null ? null : refundInfo.get("success"));
        String reason = trimToNull(refundInfo == null ? null : refundInfo.get("reason"));
        String remark = trimToNull(refundInfo == null ? null : refundInfo.get("remark"));

        if (success) {
            // 同意退款后回滚库存并关闭订单
            rollbackStock(order.getId());
            order.setStatus(OmsOrder.STATUS_CLOSED);
            orderMapper.updateById(order);

            String handleRemark = StrUtil.blankToDefault(remark, "管理员已同意退款");
            afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                    .eq(OmsAfterSale::getOrderId, order.getId())
                    .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING)
                    .set(OmsAfterSale::getStatus, OmsAfterSale.STATUS_COMPLETED)
                    .set(OmsAfterSale::getHandleRemark, handleRemark)
                    .set(OmsAfterSale::getHandleTime, LocalDateTime.now()));

            // 通知用户退款通过
            notificationService.sendToUser(
                    order.getUserId(),
                    "退款已通过",
                    "订单" + order.getOrderSn() + "退款申请已通过",
                    "order_refund",
                    order.getId()
            );
            return;
        }

        // 驳回退款后，把订单状态退回到已支付待发货。
        order.setStatus(OmsOrder.STATUS_PAID);
        orderMapper.updateById(order);

        String rejectRemark = StrUtil.blankToDefault(remark, "管理员已拒绝退款");
        afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getOrderId, order.getId())
                .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING)
                .set(OmsAfterSale::getStatus, OmsAfterSale.STATUS_REJECTED)
                .set(OmsAfterSale::getHandleRemark, rejectRemark)
                .set(OmsAfterSale::getHandleTime, LocalDateTime.now()));

        // 通知用户退款被驳回
        String reasonText = StrUtil.blankToDefault(reason, "未填写具体原因");
        notificationService.sendToUser(
                order.getUserId(),
                "退款已驳回",
                "订单" + order.getOrderSn() + "退款申请被驳回，原因：" + reasonText,
                "order_refund",
                order.getId()
        );
    }

    /**
     * 取消订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        OmsOrder order = requireOrder(id);
        if (order.getStatus() == null) {
            throw new RuntimeException("Invalid order status");
        }
        if (order.getStatus() == OmsOrder.STATUS_PENDING_PAY || order.getStatus() == OmsOrder.STATUS_PAID) {
            // 关闭订单并回滚库存
            boolean locked = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, order.getId())
                    .eq(OmsOrder::getStatus, order.getStatus())
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_CLOSED)) > 0;
            if (!locked) {
                throw new RuntimeException("Order status changed, please refresh and retry");
            }
            rollbackStock(order.getId());
            return;
        }
        if (order.getStatus() == OmsOrder.STATUS_REFUNDING) {
            throw new RuntimeException("This order is refunding. Please handle refund decision");
        }
        throw new RuntimeException("Current order status cannot be canceled");
    }

    /**
     * 修改地址
     */
    @Override
    public void updateAddress(Long id, Map<String, String> addressInfo) {
        // 更新订单收货信息
        OmsOrder order = requireOrder(id);
        if (addressInfo.containsKey("address")) {
            order.setAddress(addressInfo.get("address"));
        }
        if (addressInfo.containsKey("receiver")) {
            order.setReceiver(addressInfo.get("receiver"));
        }
        if (addressInfo.containsKey("phone")) {
            order.setPhone(addressInfo.get("phone"));
        }

        // 同步维护 receiver_info 快照。
        Map<String, String> info = new HashMap<>();
        if (StrUtil.isNotBlank(order.getReceiverInfo())) {
            try {
                info.putAll(JSON.parseObject(order.getReceiverInfo(), Map.class));
            } catch (Exception ignore) {
                // 解析失败时，按最小信息重新组装。
            }
        }
        if (StrUtil.isNotBlank(order.getReceiver())) {
            info.put("name", order.getReceiver());
        }
        if (StrUtil.isNotBlank(order.getPhone())) {
            info.put("phone", order.getPhone());
        }
        if (StrUtil.isNotBlank(order.getAddress())) {
            info.put("address", order.getAddress());
            info.put("detailAddress", order.getAddress());
        }
        info.putIfAbsent("province", "");
        info.putIfAbsent("city", "");
        info.putIfAbsent("region", "");
        order.setReceiverInfo(JSON.toJSONString(info));

        orderMapper.updateById(order);
    }

    /**
     * 导出订单
     */
    @Override
    public String export(Integer status, String startTime, String endTime) {
        return "/api/admin/order/download?timestamp=" + System.currentTimeMillis();
    }

    /**
     * 软删除订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Long id) {
        requireOrder(id);
        orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, id)
                .and(w -> w.isNull(OmsOrder::getAdminDeleted).or().eq(OmsOrder::getAdminDeleted, 0))
                .set(OmsOrder::getAdminDeleted, 1));
    }

    /**
     * 批量软删除订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSoftDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> normalizedIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        // 逐个复用单删逻辑
        for (Long id : normalizedIds) {
            softDelete(id);
        }
    }

    /**
     * 转换后台订单数据
     */
    private AdminOrderVO toVO(OmsOrder order) {
        AdminOrderVO vo = new AdminOrderVO();
        BeanUtil.copyProperties(order, vo);
        Integer effectiveStatus = normalizeOrderStatus(order);
        vo.setStatus(effectiveStatus);
        vo.setOrderNo(order.getOrderSn());
        vo.setStatusDesc(getStatusDesc(effectiveStatus));

        // 兼容旧数据：有些记录可能只存了 receiver_info。
        if ((StrUtil.isBlank(vo.getReceiver()) || StrUtil.isBlank(vo.getPhone()) || StrUtil.isBlank(vo.getAddress()))
                && StrUtil.isNotBlank(order.getReceiverInfo())) {
            try {
                Map<String, String> info = JSON.parseObject(order.getReceiverInfo(), Map.class);
                if (StrUtil.isBlank(vo.getReceiver())) {
                    vo.setReceiver(info.get("name"));
                }
                if (StrUtil.isBlank(vo.getPhone())) {
                    vo.setPhone(info.get("phone"));
                }
                if (StrUtil.isBlank(vo.getAddress())) {
                    String full = info.get("address");
                    if (StrUtil.isNotBlank(full)) {
                        vo.setAddress(full);
                    } else {
                        vo.setAddress(StrUtil.nullToEmpty(info.get("province"))
                                + StrUtil.nullToEmpty(info.get("city"))
                                + StrUtil.nullToEmpty(info.get("region"))
                                + StrUtil.nullToEmpty(info.get("detailAddress")));
                    }
                }
            } catch (Exception ignore) {
                // 异常 JSON 直接忽略，避免影响列表展示。
            }
        }

        SysUser user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
        }

        // 查询订单商品明细
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, order.getId()));
        vo.setItems(items.stream().map(item -> {
            AdminOrderVO.OrderItemVO itemVO = new AdminOrderVO.OrderItemVO();
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductImage(item.getProductImg());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            return itemVO;
        }).toList());

        // 查询当前订单关联的退款申请
        OmsAfterSale refundReq = afterSaleMapper.selectOne(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getOrderId, order.getId())
                .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING)
                .orderByDesc(OmsAfterSale::getCreateTime)
                .last("limit 1"));
        if (refundReq != null) {
            AdminOrderVO.RefundVO refundVO = new AdminOrderVO.RefundVO();
            refundVO.setAfterSaleId(refundReq.getId());
            refundVO.setReason(refundReq.getReason());
            refundVO.setDescription(refundReq.getDescription());
            refundVO.setEvidenceImages(parseEvidence(refundReq.getEvidenceImages()));
            refundVO.setStatus(refundReq.getStatus());
            refundVO.setStatusDesc(getAfterSaleStatusDesc(refundReq.getStatus()));
            vo.setRefund(refundVO);
        }

        return vo;
    }

    /**
     * 回滚库存
     */
    private void rollbackStock(Long orderId) {
        // 查询订单商品项
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, orderId));
        for (OmsOrderItem item : items) {
            Integer qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (item.getProductId() == null || qty <= 0) {
                continue;
            }
            // 把库存补回商品表
            productService.update(new LambdaUpdateWrapper<PmsProduct>()
                    .eq(PmsProduct::getId, item.getProductId())
                    .setSql("stock = stock + " + qty));
        }
    }

    /**
     * 查询订单
     */
    private OmsOrder requireOrder(Long id) {
        OmsOrder order = orderMapper.selectById(id);
        if (order == null || Integer.valueOf(1).equals(order.getAdminDeleted())) {
            throw new RuntimeException("Order not found");
        }
        return order;
    }

    /**
     * 规范订单状态
     */
    private Integer normalizeOrderStatus(OmsOrder order) {
        Integer status = order.getStatus();
        boolean hasShipInfo = order.getShipTime() != null
                || StrUtil.isNotBlank(order.getTrackingNo())
                || StrUtil.isNotBlank(order.getShipCompany());
        boolean hasPayInfo = order.getPayTime() != null;

        if (hasShipInfo && (status == null
                || status == OmsOrder.STATUS_PENDING_PAY
                || status == OmsOrder.STATUS_PAID)) {
            return OmsOrder.STATUS_SHIPPED;
        }
        if (hasPayInfo && (status == null || status == OmsOrder.STATUS_PENDING_PAY)) {
            return OmsOrder.STATUS_PAID;
        }
        return status == null ? OmsOrder.STATUS_PENDING_PAY : status;
    }

    /**
     * 解析布尔值
     */
    private boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    /**
     * 去掉空白并转成空值
     */
    private String trimToNull(Object value) {
        return value == null ? null : StrUtil.trimToNull(String.valueOf(value));
    }

    /**
     * 解析售后凭证图片
     */
    private List<String> parseEvidence(String evidenceJson) {
        if (StrUtil.isBlank(evidenceJson)) {
            return Collections.emptyList();
        }
        try {
            List<String> list = JSON.parseArray(evidenceJson, String.class);
            return list == null ? Collections.emptyList() : list;
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }

    /**
     * 获取订单状态说明
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case OmsOrder.STATUS_PENDING_PAY -> "Pending payment";
            case OmsOrder.STATUS_PAID -> "Pending shipment";
            case OmsOrder.STATUS_SHIPPED -> "Shipped";
            case OmsOrder.STATUS_COMPLETED -> "Completed";
            case OmsOrder.STATUS_CLOSED -> "Closed";
            case OmsOrder.STATUS_REFUNDING -> "Refunding";
            default -> "Unknown";
        };
    }

    /**
     * 获取售后状态说明
     */
    private String getAfterSaleStatusDesc(Integer status) {
        if (status == null) {
            return "Processing";
        }
        return switch (status) {
            case OmsAfterSale.STATUS_PENDING -> "Pending";
            case OmsAfterSale.STATUS_PROCESSING -> "Processing";
            case OmsAfterSale.STATUS_COMPLETED -> "Completed";
            case OmsAfterSale.STATUS_REJECTED -> "Rejected";
            case OmsAfterSale.STATUS_CANCELED -> "Canceled";
            default -> "Processing";
        };
    }
}
