package org.petmeet.service.impl;

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
import org.petmeet.entity.*;
import org.petmeet.mapper.OmsAfterSaleMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.OmsCartItemService;
import org.petmeet.service.OmsOrderService;
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
    private final OmsCartItemService cartItemService;
    private final PmsProductService productService;
    private final UmsAddressService addressService;
    private final SysNotificationService notificationService;
    private final SysUserMapper userMapper;

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
            throw new RuntimeException("Please select items to checkout");
        }

        // 校验收货地址是否属于当前用户
        UmsAddress address = addressService.getById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new RuntimeException("Address not found");
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
                throw new RuntimeException("产品未找到");   }
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new RuntimeException("产品不打折: " + product.getName()); }
            if (product.getStock() == null || product.getStock() < cart.getQuantity()) {
                throw new RuntimeException("库存不足: " + product.getName());}
            boolean updated = productService.update(new LambdaUpdateWrapper<PmsProduct>()
                    .eq(PmsProduct::getId, product.getId())
                    .eq(PmsProduct::getVersion, product.getVersion())
                    .ge(PmsProduct::getStock, cart.getQuantity())
                    .setSql("stock = stock - " + cart.getQuantity())
                    .setSql("version = version + 1"));
            if (!updated) {
                throw new RuntimeException("库存已更改，请重试: " + product.getName());  }
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
        order.setStatus(OmsOrder.STATUS_PENDING_PAY);
        order.setReviewStatus(OmsOrder.REVIEW_PENDING);
        order.setReceiverInfo(JSON.toJSONString(receiverInfo));
        order.setReceiver(address.getName());
        order.setPhone(address.getPhone());
        order.setAddress(receiverInfo.get("address"));
        order.setUserDeleted(0);
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
        if (order == null || !order.getUserId().equals(userId) || Integer.valueOf(1).equals(order.getUserDeleted())) {
            throw new RuntimeException("Order not found");
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
        wrapper.and(w -> w.isNull(OmsOrder::getUserDeleted).or().eq(OmsOrder::getUserDeleted, 0));
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
        Long userId = StpUtil.getLoginIdAsLong();
        OmsOrder order = requireMyOrder(orderId, userId);
        if (order.getStatus() == null || order.getStatus() != OmsOrder.STATUS_PENDING_PAY) {
            throw new RuntimeException("Order status is invalid for payment");
        }

        // 支付成功后更新订单状态
        order.setStatus(OmsOrder.STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        this.updateById(order);

        // 同步累计商品销量
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, orderId));
        for (OmsOrderItem item : items) {
            Integer qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty <= 0 || item.getProductId() == null) {
                continue;
            }
            productService.update(new LambdaUpdateWrapper<PmsProduct>()
                    .eq(PmsProduct::getId, item.getProductId())
                    .setSql("sales = sales + " + qty));
        }
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
            throw new RuntimeException("Current order status does not support cancel");
        }

        // 待付款订单直接关闭，并回滚库存。
        if (status == OmsOrder.STATUS_PENDING_PAY) {
            boolean locked = this.update(new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, orderId)
                    .eq(OmsOrder::getUserId, userId)
                    .eq(OmsOrder::getStatus, OmsOrder.STATUS_PENDING_PAY)
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_CLOSED));
            if (!locked) {
                throw new RuntimeException("Order status changed, please refresh and retry");
            }
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
                throw new RuntimeException("Order status changed, please refresh and retry");
            }

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
            throw new RuntimeException("Refund is processing, do not submit repeatedly");
        }

        throw new RuntimeException("Current order status does not support cancel");
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
            throw new RuntimeException("Current order status cannot confirm receipt");
        }

        order.setStatus(OmsOrder.STATUS_COMPLETED);
        if (order.getReviewStatus() == null) {
            order.setReviewStatus(OmsOrder.REVIEW_PENDING);
        }
        this.updateById(order);
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
            throw new RuntimeException("Please confirm receipt first");
        }

        Integer current = order.getReviewStatus() == null ? OmsOrder.REVIEW_PENDING : order.getReviewStatus();
        if (current == OmsOrder.REVIEW_DONE) {
            throw new RuntimeException("Order already reviewed");
        }

        // 保存本次评价信息
        order.setReviewStatus(OmsOrder.REVIEW_DONE);
        order.setReviewScore(dto.getScore());
        order.setReviewContent(StrUtil.trimToNull(dto.getContent()));
        order.setReviewTime(LocalDateTime.now());
        this.updateById(order);
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
            throw new RuntimeException("Review does not exist");
        }

        // 清空订单上的评价数据
        order.setReviewStatus(OmsOrder.REVIEW_PENDING);
        order.setReviewScore(null);
        order.setReviewContent(null);
        order.setReviewTime(null);
        this.updateById(order);
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
            throw new RuntimeException("仅已完成或已关闭的订单可删除");
        }

        // 校验订单是否还有处理中售后
        Long activeAfterSaleCount = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getOrderId, orderId)
                .eq(OmsAfterSale::getUserId, userId)
                .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING));
        if (activeAfterSaleCount != null && activeAfterSaleCount > 0) {
            throw new RuntimeException("订单存在处理中售后，暂不可删除");
        }

        // 逻辑删除订单，前端不再展示
        order.setUserDeleted(1);
        this.updateById(order);
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
     * 自动创建退款申请
     */
    private void ensureAutoRefundRequest(OmsOrder order, Long userId) {
        // 查询订单明细，为每个商品补建售后申请
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, order.getId()));
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Order items not found");
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
                .eq(SysUser::getRole, "admin")
                .eq(SysUser::getStatus, 1));
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
        if (order == null || !order.getUserId().equals(userId) || Integer.valueOf(1).equals(order.getUserDeleted())) {
            throw new RuntimeException("Order not found");
        }
        return order;
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(OmsOrder order) {
        if (order == null || order.getStatus() == null) {
            return "Unknown";
        }
        return switch (order.getStatus()) {
            case OmsOrder.STATUS_PENDING_PAY -> "Pending payment";
            case OmsOrder.STATUS_PAID -> "Pending shipment";
            case OmsOrder.STATUS_SHIPPED -> "Pending receipt";
            case OmsOrder.STATUS_COMPLETED -> {
                Integer rs = order.getReviewStatus() == null ? OmsOrder.REVIEW_PENDING : order.getReviewStatus();
                yield rs == OmsOrder.REVIEW_PENDING ? "Pending review" : "Completed";
            }
            case OmsOrder.STATUS_CLOSED -> "Closed";
            case OmsOrder.STATUS_REFUNDING -> "Refunding";
            default -> "Unknown";
        };
    }
}
