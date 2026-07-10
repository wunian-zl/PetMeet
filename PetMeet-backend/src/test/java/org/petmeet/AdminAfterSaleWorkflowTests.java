package org.petmeet;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.petmeet.dto.AdminAfterSaleActionDTO;
import org.petmeet.dto.AfterSaleApplyDTO;
import org.petmeet.dto.AfterSaleReturnLogisticsDTO;
import org.petmeet.dto.OrderSubmitDTO;
import org.petmeet.dto.PayCreateDTO;
import org.petmeet.entity.OmsAfterSale;
import org.petmeet.entity.OmsCartItem;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.OmsRefundLog;
import org.petmeet.entity.PmsCategory;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysUser;
import org.petmeet.entity.UmsAddress;
import org.petmeet.enums.RefundStatusEnum;
import org.petmeet.mapper.OmsAfterSaleMapper;
import org.petmeet.mapper.OmsCartItemMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.OmsRefundLogMapper;
import org.petmeet.mapper.PmsCategoryMapper;
import org.petmeet.mapper.PmsProductMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.mapper.UmsAddressMapper;
import org.petmeet.service.AdminAfterSaleService;
import org.petmeet.service.AdminOrderService;
import org.petmeet.service.OmsAfterSaleService;
import org.petmeet.service.OmsOrderService;
import org.petmeet.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class AdminAfterSaleWorkflowTests {

    @Autowired
    private AdminAfterSaleService adminAfterSaleService;

    @Autowired
    private AdminOrderService adminOrderService;

    @Autowired
    private OmsAfterSaleService afterSaleService;

    @Autowired
    private OmsOrderService orderService;

    @Autowired
    private PayService payService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private UmsAddressMapper addressMapper;

    @Autowired
    private PmsCategoryMapper categoryMapper;

    @Autowired
    private PmsProductMapper productMapper;

    @Autowired
    private OmsCartItemMapper cartItemMapper;

    @Autowired
    private OmsOrderMapper orderMapper;

    @Autowired
    private OmsOrderItemMapper orderItemMapper;

    @Autowired
    private OmsAfterSaleMapper afterSaleMapper;

    @Autowired
    private OmsRefundLogMapper refundLogMapper;

    private Long loginId;

    @AfterEach
    void clearLoginState() {
        if (loginId != null) {
            StpUtil.kickout(loginId);
        }
    }

    @Test
    void refundOnlyRefundsSingleOrderItemAmount() {
        TestOrder fixture = createPaidOrder();
        Long afterSaleId = applyAfterSale(fixture, 0);

        adminAfterSaleService.approveRefund(afterSaleId, action("同意仅退款"));

        OmsAfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        assertEquals(OmsAfterSale.STATUS_COMPLETED, afterSale.getStatus());
        assertEquals(0, new BigDecimal("19.90").compareTo(afterSale.getRefundAmount()));

        OmsOrder order = orderMapper.selectById(fixture.orderId());
        assertEquals(0, new BigDecimal("19.90").compareTo(order.getRefundAmount()));
        assertEquals(OmsOrder.STATUS_PAID, order.getStatus());

        OmsRefundLog refundLog = refundLogMapper.selectOne(new LambdaQueryWrapper<OmsRefundLog>()
                .eq(OmsRefundLog::getAfterSaleId, afterSaleId)
                .last("limit 1"));
        assertNotNull(refundLog);
        assertEquals(RefundStatusEnum.SUCCESS.getCode(), refundLog.getRefundStatus());
    }

    @Test
    void returnRefundWaitsForBuyerLogisticsThenRefunds() {
        TestOrder fixture = createPaidOrder();
        Long afterSaleId = applyAfterSale(fixture, 1);

        AdminAfterSaleActionDTO approve = action("同意退货");
        approve.setReturnAddress("四川省成都市退货路1号");
        adminAfterSaleService.approveReturn(afterSaleId, approve);

        OmsAfterSale waitingBuyer = afterSaleMapper.selectById(afterSaleId);
        assertEquals(OmsAfterSale.STATUS_WAIT_BUYER_RETURN, waitingBuyer.getStatus());

        AfterSaleReturnLogisticsDTO logistics = new AfterSaleReturnLogisticsDTO();
        logistics.setCompany("顺丰速运");
        logistics.setTrackingNo("SF123456");
        afterSaleService.submitReturnLogistics(afterSaleId, logistics);

        OmsAfterSale waitingMerchant = afterSaleMapper.selectById(afterSaleId);
        assertEquals(OmsAfterSale.STATUS_WAIT_MERCHANT_RECEIVE, waitingMerchant.getStatus());

        adminAfterSaleService.confirmReturnRefund(afterSaleId, action("确认收货并退款"));
        OmsAfterSale completed = afterSaleMapper.selectById(afterSaleId);
        assertEquals(OmsAfterSale.STATUS_COMPLETED, completed.getStatus());
    }

    @Test
    void exchangeRecordsExchangeShipmentAndUserCompletion() {
        TestOrder fixture = createPaidOrder();
        Long afterSaleId = applyAfterSale(fixture, 2);

        AdminAfterSaleActionDTO approve = action("同意换货退回");
        approve.setReturnAddress("四川省成都市换货仓");
        adminAfterSaleService.approveReturn(afterSaleId, approve);

        AfterSaleReturnLogisticsDTO logistics = new AfterSaleReturnLogisticsDTO();
        logistics.setCompany("中通快递");
        logistics.setTrackingNo("ZT123456");
        afterSaleService.submitReturnLogistics(afterSaleId, logistics);
        adminAfterSaleService.confirmReturnExchange(afterSaleId, action("确认收货"));

        AdminAfterSaleActionDTO ship = action("换货发出");
        ship.setExchangeCompany("圆通速递");
        ship.setExchangeTrackingNo("YT123456");
        adminAfterSaleService.shipExchange(afterSaleId, ship);

        OmsAfterSale shipped = afterSaleMapper.selectById(afterSaleId);
        assertEquals(OmsAfterSale.STATUS_EXCHANGE_SHIPPED, shipped.getStatus());
        assertEquals("圆通速递", shipped.getExchangeCompany());

        afterSaleService.complete(afterSaleId);
        OmsAfterSale completed = afterSaleMapper.selectById(afterSaleId);
        assertEquals(OmsAfterSale.STATUS_COMPLETED, completed.getStatus());
    }

    @Test
    void exchangeCannotShipBeforeReturnReceived() {
        TestOrder fixture = createPaidOrder();
        Long afterSaleId = applyAfterSale(fixture, 2);

        afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, afterSaleId)
                .set(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PROCESSING)
                .set(OmsAfterSale::getReturnReceiveTime, null));

        AdminAfterSaleActionDTO ship = action("尝试发货");
        ship.setExchangeCompany("圆通速递");
        ship.setExchangeTrackingNo("YT123456");
        assertThrows(RuntimeException.class, () -> adminAfterSaleService.shipExchange(afterSaleId, ship));
    }

    @Test
    void userCannotCancelAfterReturnLogisticsSubmitted() {
        TestOrder fixture = createPaidOrder();
        Long afterSaleId = applyAfterSale(fixture, 1);

        AdminAfterSaleActionDTO approve = action("同意退货");
        approve.setReturnAddress("四川省成都市退货路1号");
        adminAfterSaleService.approveReturn(afterSaleId, approve);

        AfterSaleReturnLogisticsDTO logistics = new AfterSaleReturnLogisticsDTO();
        logistics.setCompany("顺丰速运");
        logistics.setTrackingNo("SF123456");
        afterSaleService.submitReturnLogistics(afterSaleId, logistics);

        assertThrows(RuntimeException.class, () -> afterSaleService.cancel(afterSaleId));
    }

    @Test
    void legacyStatusApiIsOnlyForOrderRefundCompatibility() {
        TestOrder fixture = createPaidOrder();
        Long afterSaleId = applyAfterSale(fixture, 0);

        orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, fixture.orderId())
                .set(OmsOrder::getStatus, OmsOrder.STATUS_REFUNDING));

        assertThrows(RuntimeException.class,
                () -> adminAfterSaleService.updateStatus(afterSaleId, OmsAfterSale.STATUS_COMPLETED, "旧接口完成"));
    }

    @Test
    void orderRefundEntryRedirectsActiveAfterSaleToWorkbench() {
        TestOrder fixture = createPaidOrder();
        applyAfterSale(fixture, 0);

        orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, fixture.orderId())
                .set(OmsOrder::getStatus, OmsOrder.STATUS_REFUNDING));

        assertThrows(RuntimeException.class,
                () -> adminOrderService.refund(fixture.orderId(), Map.of("success", true)));
        OmsRefundLog refundLog = refundLogMapper.selectOne(new LambdaQueryWrapper<OmsRefundLog>()
                .eq(OmsRefundLog::getOrderId, fixture.orderId())
                .last("limit 1"));
        assertNull(refundLog);
    }

    @Test
    void duplicateRefundForSameAfterSaleIsBlocked() {
        TestOrder fixture = createPaidOrder();
        Long afterSaleId = applyAfterSale(fixture, 0);

        payService.refundOrder(fixture.orderId(), afterSaleId, new BigDecimal("19.90"), "首次退款");

        assertThrows(RuntimeException.class,
                () -> payService.refundOrder(fixture.orderId(), afterSaleId, new BigDecimal("19.90"), "重复退款"));
    }

    @Test
    void userCanCancelPendingOrderRefundAndRestorePaidOrder() {
        TestOrder fixture = createPaidOrder();

        orderService.cancel(fixture.orderId());

        OmsOrder refundingOrder = orderMapper.selectById(fixture.orderId());
        assertEquals(OmsOrder.STATUS_REFUNDING, refundingOrder.getStatus());
        Long activeBeforeCancel = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getOrderId, fixture.orderId())
                .in(OmsAfterSale::getStatus, OmsAfterSale.activeStatuses()));
        assertEquals(2L, activeBeforeCancel);

        orderService.cancelRefund(fixture.orderId());

        OmsOrder restoredOrder = orderMapper.selectById(fixture.orderId());
        assertEquals(OmsOrder.STATUS_PAID, restoredOrder.getStatus());
        Long activeAfterCancel = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getOrderId, fixture.orderId())
                .in(OmsAfterSale::getStatus, OmsAfterSale.activeStatuses()));
        assertEquals(0L, activeAfterCancel);
        Long canceledAfterSales = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getOrderId, fixture.orderId())
                .eq(OmsAfterSale::getStatus, OmsAfterSale.STATUS_CANCELED));
        assertEquals(2L, canceledAfterSales);
    }

    private Long applyAfterSale(TestOrder fixture, int type) {
        AfterSaleApplyDTO dto = new AfterSaleApplyDTO();
        dto.setOrderId(fixture.orderId());
        dto.setOrderItemId(fixture.orderItemId());
        dto.setType(type);
        dto.setReason("测试售后");
        Long id = afterSaleService.apply(dto);
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        assertEquals(0, new BigDecimal("19.90").compareTo(afterSale.getRefundAmount()));
        return id;
    }

    private AdminAfterSaleActionDTO action(String remark) {
        AdminAfterSaleActionDTO dto = new AdminAfterSaleActionDTO();
        dto.setRemark(remark);
        return dto;
    }

    private TestOrder createPaidOrder() {
        SysUser user = createUser();
        UmsAddress address = createAddress(user.getId());
        PmsCategory category = createCategory();
        PmsProduct firstProduct = createProduct(category.getId(), "售后测试商品A");
        PmsProduct secondProduct = createProduct(category.getId(), "售后测试商品B");
        OmsCartItem first = createCartItem(user.getId(), firstProduct.getId());
        OmsCartItem second = createCartItem(user.getId(), secondProduct.getId());

        loginId = user.getId();
        StpUtil.login(loginId);

        OrderSubmitDTO orderDTO = new OrderSubmitDTO();
        orderDTO.setAddressId(address.getId());
        orderDTO.setCartItemIds(List.of(first.getId(), second.getId()));
        Long orderId = orderService.submitOrder(orderDTO);

        PayCreateDTO payDTO = new PayCreateDTO();
        payDTO.setOrderId(orderId);
        payDTO.setPayType("WECHAT_MOCK");
        payDTO.setPayMode("QR_CODE");
        payService.mockConfirm(payService.createPay(payDTO).getPaySn());

        OmsOrderItem firstItem = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                        .eq(OmsOrderItem::getOrderId, orderId)
                        .orderByAsc(OmsOrderItem::getId))
                .get(0);
        return new TestOrder(orderId, firstItem.getId());
    }

    private SysUser createUser() {
        SysUser user = new SysUser();
        user.setUsername("after_sale_test_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPassword("test-only");
        user.setNickname("after sale test");
        user.setRole("user");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private UmsAddress createAddress(Long userId) {
        UmsAddress address = new UmsAddress();
        address.setUserId(userId);
        address.setName("售后测试用户");
        address.setPhone("13800138000");
        address.setProvince("四川省");
        address.setCity("成都市");
        address.setRegion("高新区");
        address.setDetailAddress("售后测试路1号");
        address.setIsDefault(1);
        addressMapper.insert(address);
        return address;
    }

    private PmsCategory createCategory() {
        PmsCategory category = new PmsCategory();
        category.setName("售后测试分类");
        category.setSort(0);
        category.setStatus(1);
        categoryMapper.insert(category);
        return category;
    }

    private PmsProduct createProduct(Long categoryId, String name) {
        PmsProduct product = new PmsProduct();
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setPrice(new BigDecimal("19.90"));
        product.setStock(10);
        product.setVersion(0);
        product.setCoverImg("/images/after-sale-test.png");
        product.setStatus(1);
        product.setIsDeleted(0);
        product.setSales(0);
        product.setViews(0);
        product.setRelatedNoteCount(0);
        product.setCreateTime(LocalDateTime.now());
        productMapper.insert(product);
        return product;
    }

    private OmsCartItem createCartItem(Long userId, Long productId) {
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setUserId(userId);
        cartItem.setProductId(productId);
        cartItem.setQuantity(1);
        cartItem.setSelected(true);
        cartItem.setIsDeleted(0);
        cartItem.setCreateTime(LocalDateTime.now());
        cartItemMapper.insert(cartItem);
        return cartItem;
    }

    private record TestOrder(Long orderId, Long orderItemId) {
    }
}
