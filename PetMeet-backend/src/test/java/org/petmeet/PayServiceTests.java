package org.petmeet;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.petmeet.common.AppException;
import org.petmeet.dto.OrderSubmitDTO;
import org.petmeet.dto.PayCreateDTO;
import org.petmeet.entity.OmsCartItem;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsPayLog;
import org.petmeet.entity.PmsCategory;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysUser;
import org.petmeet.entity.UmsAddress;
import org.petmeet.enums.PayStatusEnum;
import org.petmeet.enums.PayTypeEnum;
import org.petmeet.mapper.OmsCartItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.OmsPayLogMapper;
import org.petmeet.mapper.PmsCategoryMapper;
import org.petmeet.mapper.PmsProductMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.mapper.UmsAddressMapper;
import org.petmeet.service.OmsOrderService;
import org.petmeet.service.PayService;
import org.petmeet.vo.PayResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class PayServiceTests {

    @Autowired
    private PayService payService;

    @Autowired
    private OmsOrderService orderService;

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
    private OmsPayLogMapper payLogMapper;

    private Long loginId;

    @AfterEach
    void clearLoginState() {
        if (loginId != null) {
            StpUtil.kickout(loginId);
        }
    }

    @Test
    void createMockPayIsIdempotentAndConfirmUpdatesOrder() {
        TestOrder fixture = createPendingOrder();
        PayCreateDTO dto = mockPayDto(fixture.orderId);

        PayResponseVO first = payService.createPay(dto);
        PayResponseVO second = payService.createPay(dto);

        assertEquals(first.getPaySn(), second.getPaySn());
        assertEquals("WECHAT_MOCK", first.getPayType());

        payService.mockConfirm(first.getPaySn());

        OmsOrder paidOrder = orderMapper.selectById(fixture.orderId);
        assertEquals(OmsOrder.STATUS_PAID, paidOrder.getStatus());
        assertEquals(PayTypeEnum.WECHAT_MOCK.getCode(), paidOrder.getPayType());
        assertEquals(first.getPaySn(), paidOrder.getPaySn());
        assertNotNull(paidOrder.getPayTime());

        PmsProduct paidProduct = productMapper.selectById(fixture.productId);
        assertEquals(2, paidProduct.getSales());
    }

    @Test
    void expiredPendingPayCreatesNewLogAndClosesOldLog() {
        TestOrder fixture = createPendingOrder();
        PayCreateDTO dto = mockPayDto(fixture.orderId);
        PayResponseVO first = payService.createPay(dto);

        payLogMapper.update(null, new LambdaUpdateWrapper<OmsPayLog>()
                .eq(OmsPayLog::getPaySn, first.getPaySn())
                .set(OmsPayLog::getExpireTime, LocalDateTime.now().minusMinutes(1)));

        PayResponseVO second = payService.createPay(dto);

        assertNotEquals(first.getPaySn(), second.getPaySn());
        OmsPayLog oldLog = payLogMapper.selectOne(new LambdaQueryWrapper<OmsPayLog>()
                .eq(OmsPayLog::getPaySn, first.getPaySn()));
        assertEquals(PayStatusEnum.CLOSED.getCode(), oldLog.getPayStatus());
    }

    @Test
    void nonOwnerCannotCreatePay() {
        TestOrder fixture = createPendingOrder();
        SysUser other = createUser();
        StpUtil.kickout(loginId);
        loginId = other.getId();
        StpUtil.login(loginId);

        PayCreateDTO dto = mockPayDto(fixture.orderId);

        assertThrows(AppException.class, () -> payService.createPay(dto));
    }

    private TestOrder createPendingOrder() {
        SysUser user = createUser();
        UmsAddress address = createAddress(user.getId());
        PmsCategory category = createCategory();
        PmsProduct product = createProduct(category.getId());
        OmsCartItem cartItem = createCartItem(user.getId(), product.getId());

        loginId = user.getId();
        StpUtil.login(loginId);

        OrderSubmitDTO orderDTO = new OrderSubmitDTO();
        orderDTO.setAddressId(address.getId());
        orderDTO.setCartItemIds(List.of(cartItem.getId()));
        Long orderId = orderService.submitOrder(orderDTO);
        return new TestOrder(orderId, product.getId());
    }

    private PayCreateDTO mockPayDto(Long orderId) {
        PayCreateDTO dto = new PayCreateDTO();
        dto.setOrderId(orderId);
        dto.setPayType("WECHAT_MOCK");
        dto.setPayMode("QR_CODE");
        return dto;
    }

    private SysUser createUser() {
        SysUser user = new SysUser();
        user.setUsername("pay_test_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPassword("test-only");
        user.setNickname("pay test");
        user.setRole("user");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private UmsAddress createAddress(Long userId) {
        UmsAddress address = new UmsAddress();
        address.setUserId(userId);
        address.setName("支付测试用户");
        address.setPhone("13800138000");
        address.setProvince("四川省");
        address.setCity("成都市");
        address.setRegion("郫都区");
        address.setDetailAddress("支付测试路1号");
        address.setIsDefault(1);
        addressMapper.insert(address);
        return address;
    }

    private PmsCategory createCategory() {
        PmsCategory category = new PmsCategory();
        category.setName("支付测试分类");
        category.setSort(0);
        category.setStatus(1);
        categoryMapper.insert(category);
        return category;
    }

    private PmsProduct createProduct(Long categoryId) {
        PmsProduct product = new PmsProduct();
        product.setCategoryId(categoryId);
        product.setName("支付测试商品");
        product.setPrice(new BigDecimal("19.90"));
        product.setStock(10);
        product.setVersion(0);
        product.setCoverImg("/images/pay-test.png");
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
        cartItem.setQuantity(2);
        cartItem.setSelected(true);
        cartItem.setIsDeleted(0);
        cartItem.setCreateTime(LocalDateTime.now());
        cartItemMapper.insert(cartItem);
        return cartItem;
    }

    private record TestOrder(Long orderId, Long productId) {
    }
}
