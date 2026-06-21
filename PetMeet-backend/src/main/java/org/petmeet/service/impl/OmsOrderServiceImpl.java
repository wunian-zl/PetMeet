package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.dto.OrderReviewDTO;
import org.petmeet.dto.OrderSubmitDTO;
import org.petmeet.dto.PayCreateDTO;
import org.petmeet.entity.*;
import org.petmeet.enums.PayStatusEnum;
import org.petmeet.mapper.OmsAfterSaleMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.OmsPayLogMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.OmsCartItemService;
import org.petmeet.service.OmsOrderService;
import org.petmeet.service.PayService;
import org.petmeet.service.PmsProductService;
import org.petmeet.service.SysNotificationService;
import org.petmeet.service.UmsAddressService;
import org.petmeet.vo.OrderDetailVO;
import org.petmeet.vo.OrderItemVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmsOrderServiceImpl extends ServiceImpl<OmsOrderMapper, OmsOrder> implements OmsOrderService {

    private final OmsOrderItemMapper orderItemMapper;
    private final OmsAfterSaleMapper afterSaleMapper;
    private final OmsPayLogMapper payLogMapper;
    private final OmsCartItemService cartItemService;
    private final PmsProductService productService;
    private final UmsAddressService addressService;
    private final SysNotificationService notificationService;
    private final SysUserMapper userMapper;
    private final PayService payService;

    @Value("${app.order.pay-timeout-minutes:30}")
    private Integer payTimeoutMinutes;

    /**
     * 提交订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitOrder(OrderSubmitDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 根据购物车项查询本次结算商品
        LambdaQueryWrapper<OmsCartItem> cartWrapper = new LambdaQueryWrapper<>();
        cartWrapper.eq(OmsCartItem::getUserId, userId).in(OmsCartItem::getId, dto.getCartItemIds());
        List<OmsCartItem> cartItems = cartItemService.list(cartWrapper);
        if (cartItems.isEmpty()) {
            throw AppException.badRequest("请选择要结算的商品");
        }

        // 校验收货地址是否属于当前用户
        UmsAddress address = addressService.getById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw AppException.notFound("收货地址不存在");
        }

        // 批量查询商品信息
        Set<Long> productIds = cartItems.stream().map(OmsCartItem::getProductId).collect(Collectors.toSet());
        Map<Long, PmsProduct> productMap = productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(PmsProduct::getId, p -> p));
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OmsOrderItem> orderItems = new ArrayList<>();
        // 逐个校验库存并生成订单明细
        for (OmsCartItem cart : cartItems) {
            PmsProduct product = productMap.get(cart.getProductId());
            if (product == null) {
                throw AppException.notFound("商品不存在");   }
            if (product.getStatus() == null || product.getStatus() != PmsProduct.STATUS_ON_SHELF) {
                throw AppException.badRequest("商品已下架:" + product.getName()); }
            if (product.getStock() == null || product.getStock() < cart.getQuantity()) {
                throw AppException.badRequest("库存不足: " + product.getName());}
            boolean updated = productService.update(new LambdaUpdateWrapper<PmsProduct>()
                    .eq(PmsProduct::getId, product.getId())
                    .eq(PmsProduct::getVersion, product.getVersion())
                    .ge(PmsProduct::getStock, cart.getQuantity())
                    .setSql("stock = stock - " + cart.getQuantity())
                    .setSql("version = version + 1"));
            if (!updated) {
                throw AppException.conflict("库存已变化,请重试:" + product.getName());  }
            BigDecimal price = product.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
            OmsOrderItem item = new OmsOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImg(product.getCoverImg());
            item.setPrice(price);
            item.setQuantity(cart.getQuantity());
            orderItems.add(item); }


        // 生成订单编号
        String orderSn = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + IdUtil.simpleUUID().substring(0, 6).toUpperCase();

        // 组装收货人快照信息
        Map<String, String> receiverInfo = new HashMap<>();
        receiverInfo.put("name", address.getName());
        receiverInfo.put("phone", address.getPhone());
        receiverInfo.put("province", address.getProvince());
        receiverInfo.put("city", address.getCity());
        receiverInfo.put("region", address.getRegion());
        receiverInfo.put("detailAddress", address.getDetailAddress());
        receiverInfo.put("address", address.getProvince() + address.getCity() + address.getRegion() + address.getDetailAddress());

        // 保存订单主表
        OmsOrder order = new OmsOrder();
        order.setOrderSn(orderSn);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setStatus(OmsOrder.STATUS_PENDING_PAY);
        order.setReviewStatus(OmsOrder.REVIEW_PENDING);
        order.setReceiverInfo(JSON.toJSONString(receiverInfo));
        order.setReceiver(address.getName());
        order.setPhone(address.getPhone());
        order.setAddress(receiverInfo.get("address"));
        order.setRemark(StrUtil.trimToNull(dto.getRemark()));
        order.setUserDeleted(OmsOrder.DELETE_VISIBLE);
        order.setCreateTime(LocalDateTime.now());
        this.save(order);

        // 保存订单明细
        for (OmsOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 下单成功后删除已结算的购物车项
        cartItemService.batchDelete(dto.getCartItemIds());
        return order.getId();
    }

    /**
     * 订单详情
     */
    @Override
    public OrderDetailVO getOrderDetail(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 校验订单是否属于当前用户
        OmsOrder order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)
                || Integer.valueOf(OmsOrder.DELETE_DELETED).equals(order.getUserDeleted())) {
            throw AppException.notFound("订单不存在");
        }
        // 转成前端需要的详情数据
        return convertToDetailVO(order);
    }

    /**
     * 我的订单列表
     */
    @Override
    public Page<OrderDetailVO> pageMyOrders(Integer pageNum, Integer pageSize, Integer status, Integer reviewStatus) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 按当前用户和筛选条件查询订单
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getUserId, userId);
        wrapper.and(w -> w.isNull(OmsOrder::getUserDeleted).or().eq(OmsOrder::getUserDeleted, OmsOrder.DELETE_VISIBLE));
        if (status != null) {
            wrapper.eq(OmsOrder::getStatus, status);
        }
        if (reviewStatus != null) {
            wrapper.eq(OmsOrder::getReviewStatus, reviewStatus);
        }
        wrapper.orderByDesc(OmsOrder::getCreateTime);

        Page<OmsOrder> page = new Page<>(pageNum, pageSize);
        this.page(page, wrapper);

        // 把订单分页结果转换成详情分页结果
        Page<OrderDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToDetailVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 模拟支付
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        PayCreateDTO dto = new PayCreateDTO();
        dto.setOrderId(orderId);
        dto.setPayType("WECHAT_MOCK");
        dto.setPayMode("QR_CODE");
        payService.mockConfirm(payService.createPay(dto).getPaySn());
    }

    /**
     * 取消订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsOrder order = requireMyOrder(orderId, userId);

        Integer status = order.getStatus();
        if (status == null) {
            throw AppException.conflict("当前订单状态不允许取消");
        }

        // 待付款订单直接关闭，并回滚库存。
        if (status == OmsOrder.STATUS_PENDING_PAY) {
            boolean locked = this.update(new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, orderId)
                    .eq(OmsOrder::getUserId, userId)
                    .eq(OmsOrder::getStatus, OmsOrder.STATUS_PENDING_PAY)
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_CLOSED));
            if (!locked) {
                throw AppException.conflict("订单状态已变化,请刷新后重试");
            }
            closePendingPayLogs(orderId, "订单已取消");
            rollbackStock(orderId);
            return;
        }

        // 已支付但还没发货时，取消订单会转成退款中，并补建仅退款申请。
        if (status == OmsOrder.STATUS_PAID) {
            boolean locked = this.update(new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, orderId)
                    .eq(OmsOrder::getUserId, userId)
                    .eq(OmsOrder::getStatus, OmsOrder.STATUS_PAID)
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_REFUNDING));
            if (!locked) {
                throw AppException.conflict("订单状态已变化,请刷新后重试");
            }
            closePendingPayLogs(orderId, "订单已进入退款处理中");

            ensureAutoRefundRequest(order, userId);
            notifyAdminsForRefundingOrder(order);
            notificationService.sendToUser(
                    userId,
                    "取消申请已提交",
                    "订单" + order.getOrderSn() + "已进入退款处理中状态",
                    "order_refund",
                    order.getId()
            );
            return;
        }

        if (status == OmsOrder.STATUS_REFUNDING) {
            throw AppException.conflict("退款正在处理中,请勿重复提交");
        }

        throw AppException.conflict("当前订单状态不允许取消");
    }

    /**
     * 确认收货
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsOrder order = requireMyOrder(orderId, userId);
        if (order.getStatus() == null || order.getStatus() != OmsOrder.STATUS_SHIPPED) {
            throw AppException.conflict("当前订单状态不允许确认收货");
        }

        boolean confirmed = this.update(new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, orderId)
                .eq(OmsOrder::getUserId, userId)
                .eq(OmsOrder::getStatus, OmsOrder.STATUS_SHIPPED)
                .set(OmsOrder::getStatus, OmsOrder.STATUS_COMPLETED)
                .set(OmsOrder::getReviewStatus, order.getReviewStatus() == null
                        ? OmsOrder.REVIEW_PENDING : order.getReviewStatus()));
        if (!confirmed) {
            throw AppException.conflict("订单状态已变化,请刷新后重试");
        }
    }

    /**
     * 提交评价
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long orderId, OrderReviewDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsOrder order = requireMyOrder(orderId, userId);
        if (order.getStatus() == null || order.getStatus() != OmsOrder.STATUS_COMPLETED) {
            throw AppException.conflict("请先确认收货");
        }

        Integer current = order.getReviewStatus() == null ? OmsOrder.REVIEW_PENDING : order.getReviewStatus();
        if (current == OmsOrder.REVIEW_DONE) {
            throw AppException.conflict("订单已评价");
        }

        // 保存本次评价信息，状态锁避免重复评价。
        boolean reviewed = this.update(new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, orderId)
                .eq(OmsOrder::getUserId, userId)
                .eq(OmsOrder::getStatus, OmsOrder.STATUS_COMPLETED)
                .and(w -> w.isNull(OmsOrder::getReviewStatus)
                        .or().eq(OmsOrder::getReviewStatus, OmsOrder.REVIEW_PENDING))
                .set(OmsOrder::getReviewStatus, OmsOrder.REVIEW_DONE)
                .set(OmsOrder::getReviewScore, dto.getScore())
                .set(OmsOrder::getReviewContent, StrUtil.trimToNull(dto.getContent()))
                .set(OmsOrder::getReviewTime, LocalDateTime.now()));
        if (!reviewed) {
            throw AppException.conflict("订单评价状态已变化,请刷新后重试");
        }
    }

    /**
     * 删除评价
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsOrder order = requireMyOrder(orderId, userId);

        Integer current = order.getReviewStatus() == null ? OmsOrder.REVIEW_PENDING : order.getReviewStatus();
        if (current != OmsOrder.REVIEW_DONE) {
            throw AppException.notFound("评价不存在");
        }

        // 清空订单上的评价数据，状态锁避免并发重复删除。
        boolean deleted = this.update(new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, orderId)
                .eq(OmsOrder::getUserId, userId)
                .eq(OmsOrder::getReviewStatus, OmsOrder.REVIEW_DONE)
                .set(OmsOrder::getReviewStatus, OmsOrder.REVIEW_PENDING)
                .set(OmsOrder::getReviewScore, null)
                .set(OmsOrder::getReviewContent, null)
                .set(OmsOrder::getReviewTime, null));
        if (!deleted) {
            throw AppException.conflict("评价状态已变化,请刷新后重试");
        }
    }

    /**
     * 删除订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyOrder(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsOrder order = requireMyOrder(orderId, userId);

        Integer status = order.getStatus();
        if (status == null || (status != OmsOrder.STATUS_COMPLETED && status != OmsOrder.STATUS_CLOSED)) {
            throw AppException.badRequest("仅已完成或已关闭的订单可删除");
        }

        // 校验订单是否还有处理中售后
        Long activeAfterSaleCount = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getOrderId, orderId)
                .eq(OmsAfterSale::getUserId, userId)
                .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING));
        if (activeAfterSaleCount != null && activeAfterSaleCount > 0) {
            throw AppException.badRequest("订单存在处理中售后，暂不可删除");
        }

        // 逻辑删除订单，前端不再展示
        boolean deleted = this.update(new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, orderId)
                .eq(OmsOrder::getUserId, userId)
                .and(w -> w.isNull(OmsOrder::getUserDeleted)
                        .or().eq(OmsOrder::getUserDeleted, OmsOrder.DELETE_VISIBLE))
                .in(OmsOrder::getStatus, OmsOrder.STATUS_COMPLETED, OmsOrder.STATUS_CLOSED)
                .set(OmsOrder::getUserDeleted, OmsOrder.DELETE_DELETED));
        if (!deleted) {
            throw AppException.conflict("订单状态已变化,请刷新后重试");
        }
    }

    /**
     * 批量删除订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteMyOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }

        List<Long> normalizedIds = orderIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        // 逐个复用单删逻辑，保证校验一致
        for (Long orderId : normalizedIds) {
            deleteMyOrder(orderId);
        }
    }

    /**
     * 自动关闭超时未支付订单
     */
    @Transactional(rollbackFor = Exception.class)
    public int autoCloseExpiredUnpaidOrders(LocalDateTime deadline) {
        // 查询所有超时未支付订单
        List<OmsOrder> expired = this.list(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getStatus, OmsOrder.STATUS_PENDING_PAY)
                .le(OmsOrder::getCreateTime, deadline));
        if (expired.isEmpty()) {
            return 0;
        }

        int closedCount = 0;
        for (OmsOrder order : expired) {
            // 用状态锁避免重复关闭同一订单
            boolean locked = this.update(new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, order.getId())
                    .eq(OmsOrder::getStatus, OmsOrder.STATUS_PENDING_PAY)
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_CLOSED));
            if (!locked) {
                continue;
            }
            // 关闭订单后回滚库存并通知用户
            closePendingPayLogs(order.getId(), "订单超时关闭");
            rollbackStock(order.getId());
            closedCount++;
            notificationService.sendToUser(
                    order.getUserId(),
                    "订单已关闭",
                    "订单" + order.getOrderSn() + "因超时未支付，系统已自动关闭",
                    "order_timeout",
                    order.getId()
            );
        }
        return closedCount;
    }

    /**
     * 转换订单详情
     */
    private OrderDetailVO convertToDetailVO(OmsOrder order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderSn(order.getOrderSn());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(getStatusDesc(order));
        vo.setReviewStatus(order.getReviewStatus() == null ? OmsOrder.REVIEW_PENDING : order.getReviewStatus());
        vo.setReviewScore(order.getReviewScore());
        vo.setReviewContent(order.getReviewContent());
        vo.setReviewTime(order.getReviewTime());
        vo.setPayTime(order.getPayTime());
        vo.setPayType(getPayTypeDesc(order.getPayType()));
        vo.setPaySn(order.getPaySn());
        vo.setCreateTime(order.getCreateTime());
        vo.setShipCompany(order.getShipCompany());
        vo.setTrackingNo(order.getTrackingNo());
        vo.setShipTime(order.getShipTime());

        int timeoutMinutes = payTimeoutMinutes == null || payTimeoutMinutes <= 0 ? 30 : payTimeoutMinutes;
        if (order.getCreateTime() != null) {
            vo.setPayExpireTime(order.getCreateTime().plusMinutes(timeoutMinutes));
        }

        if (order.getReceiverInfo() != null) {
            // 解析收货地址快照
            Map<String, String> info = JSON.parseObject(order.getReceiverInfo(), Map.class);
            vo.setReceiverName(info.get("name"));
            vo.setReceiverPhone(info.get("phone"));
            String full = info.get("address");
            if (StrUtil.isNotBlank(full)) {
                vo.setReceiverAddress(full);
            } else {
                String p = StrUtil.nullToEmpty(info.get("province"));
                String c = StrUtil.nullToEmpty(info.get("city"));
                String r = StrUtil.nullToEmpty(info.get("region"));
                String d = StrUtil.nullToEmpty(info.get("detailAddress"));
                vo.setReceiverAddress(p + c + r + d);
            }
        }

        // 查询订单商品明细
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, order.getId()));

        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductImg(item.getProductImg());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return itemVO;
        }).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 回滚库存
     */
    private void rollbackStock(Long orderId) {
        // 查询订单中的商品项
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, orderId));
        for (OmsOrderItem item : items) {
            Integer quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (item.getProductId() == null || quantity <= 0) {
                continue;
            }
            // 把扣减的库存加回去
            productService.update(new LambdaUpdateWrapper<PmsProduct>()
                    .eq(PmsProduct::getId, item.getProductId())
                    .setSql("stock = stock + " + quantity));
        }
    }

    /**
     * 关闭订单关联的待支付流水，避免订单关闭后旧二维码继续悬挂。
     */
    private void closePendingPayLogs(Long orderId, String reason) {
        if (orderId == null) {
            return;
        }
        payLogMapper.update(null, new LambdaUpdateWrapper<OmsPayLog>()
                .eq(OmsPayLog::getOrderId, orderId)
                .eq(OmsPayLog::getPayStatus, PayStatusEnum.PENDING.getCode())
                .set(OmsPayLog::getPayStatus, PayStatusEnum.CLOSED.getCode())
                .set(OmsPayLog::getErrorMsg, StrUtil.blankToDefault(reason, "订单已关闭"))
                .set(OmsPayLog::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 自动创建退款申请
     */
    private void ensureAutoRefundRequest(OmsOrder order, Long userId) {
        // 查询订单明细，为每个商品补建售后申请
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, order.getId()));
        if (items == null || items.isEmpty()) {
            throw AppException.notFound("订单商品不存在");
        }

        for (OmsOrderItem item : items) {
            if (item.getId() == null) {
                continue;
            }
            // 已有处理中售后时不重复创建
            Long activeCount = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                    .eq(OmsAfterSale::getUserId, userId)
                    .eq(OmsAfterSale::getOrderItemId, item.getId())
                    .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING));
            if (activeCount != null && activeCount > 0) {
                continue;
            }

            // 自动创建仅退款申请
            OmsAfterSale afterSale = new OmsAfterSale();
            afterSale.setOrderId(order.getId());
            afterSale.setOrderItemId(item.getId());
            afterSale.setUserId(userId);
            afterSale.setType(0);
            afterSale.setReason("已支付订单发货前取消");
            afterSale.setDescription("系统自动创建仅退款申请（由取消订单触发）");
            afterSale.setStatus(OmsAfterSale.STATUS_PENDING);
            afterSale.setCreateTime(LocalDateTime.now());
            afterSaleMapper.insert(afterSale);
        }
    }

    /**
     * 通知管理员优先处理退款
     */
    private void notifyAdminsForRefundingOrder(OmsOrder order) {
        // 查询所有启用中的管理员账号
        List<SysUser> admins = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId)
                .eq(SysUser::getRole, SysUser.ROLE_ADMIN)
                .eq(SysUser::getStatus, SysUser.STATUS_ENABLED));
        if (admins == null || admins.isEmpty()) {
            return;
        }
        for (SysUser admin : admins) {
            if (admin.getId() == null) {
                continue;
            }
            // 给管理员发送优先处理通知
            notificationService.sendToUser(
                    admin.getId(),
                    "退款订单待优先处理",
                    "订单" + order.getOrderSn() + "正在退款中，请先暂停发货并优先处理退款",
                    "order_refund",
                    order.getId()
            );
        }
    }

    /**
     * 校验订单归属
     */
    private OmsOrder requireMyOrder(Long orderId, Long userId) {
        OmsOrder order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)
                || Integer.valueOf(OmsOrder.DELETE_DELETED).equals(order.getUserDeleted())) {
            throw AppException.notFound("订单不存在");
        }
        return order;
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(OmsOrder order) {
        if (order == null || order.getStatus() == null) {
            return "未知";
        }
        return switch (order.getStatus()) {
            case OmsOrder.STATUS_PENDING_PAY -> "待付款";
            case OmsOrder.STATUS_PAID -> "待发货";
            case OmsOrder.STATUS_SHIPPED -> "待收货";
            case OmsOrder.STATUS_COMPLETED -> {
                Integer rs = order.getReviewStatus() == null ? OmsOrder.REVIEW_PENDING : order.getReviewStatus();
                yield rs == OmsOrder.REVIEW_PENDING ? "待评价" : "已完成";
            }
            case OmsOrder.STATUS_CLOSED -> "已关闭";
            case OmsOrder.STATUS_REFUNDING -> "退款中";
            default -> "未知";
        };
    }

    private String getPayTypeDesc(Integer payType) {
        if (payType == null) {
            return null;
        }
        return switch (payType) {
            case 1 -> "支付宝";
            case 2 -> "微信支付";
            default -> "未知";
        };
    }
}
