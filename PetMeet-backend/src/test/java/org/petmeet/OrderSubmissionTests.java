package org.petmeet;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.petmeet.dto.OrderSubmitDTO;
import org.petmeet.entity.OmsCartItem;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.PmsCategory;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysUser;
import org.petmeet.entity.UmsAddress;
import org.petmeet.mapper.OmsCartItemMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.PmsCategoryMapper;
import org.petmeet.mapper.PmsProductMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.mapper.UmsAddressMapper;
import org.petmeet.service.OmsOrderService;
import org.petmeet.service.PmsProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class OrderSubmissionTests {

    @Autowired
    private OmsOrderService orderService;

    @Autowired
    private PmsProductService productService;

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

    private Long loginId;

    @AfterEach
    void clearLoginState() {
        if (loginId != null) {
            StpUtil.kickout(loginId);
        }
    }

    @Test
    void submitOrderDeductsStockAndCreatesOrder() {
        SysUser user = createUser();
        UmsAddress address = createAddress(user.getId());
        PmsCategory category = createCategory();
        PmsProduct product = createProduct(category.getId());
        OmsCartItem cartItem = createCartItem(user.getId(), product.getId());

        loginId = user.getId();
        StpUtil.login(loginId);

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setAddressId(address.getId());
        dto.setCartItemIds(List.of(cartItem.getId()));

        Long orderId = orderService.submitOrder(dto);

        PmsProduct updatedProduct = productMapper.selectById(product.getId());
        assertEquals(8, updatedProduct.getStock());
        assertEquals(1, updatedProduct.getVersion());

        OmsOrder order = orderMapper.selectById(orderId);
        assertNotNull(order);
        assertEquals(user.getId(), order.getUserId());
        assertEquals(0, new BigDecimal("39.80").compareTo(order.getTotalAmount()));
        assertEquals(OmsOrder.STATUS_PENDING_PAY, order.getStatus());

        List<OmsOrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, orderId));
        assertEquals(1, orderItems.size());
        assertEquals(product.getId(), orderItems.get(0).getProductId());
        assertEquals(2, orderItems.get(0).getQuantity());

        OmsCartItem deletedCartItem = cartItemMapper.selectByUserAndProductIgnoreDeleted(
                user.getId(), product.getId());
        assertEquals(1, deletedCartItem.getIsDeleted());
    }

    @Test
    void staleProductVersionCannotDeductStockAgain() {
        PmsCategory category = createCategory();
        PmsProduct product = createProduct(category.getId());
        Integer initialVersion = product.getVersion();

        assertTrue(productService.deductStock(product.getId(), 2));

        boolean staleUpdateSucceeded = productService.update(new LambdaUpdateWrapper<PmsProduct>()
                .eq(PmsProduct::getId, product.getId())
                .eq(PmsProduct::getVersion, initialVersion)
                .ge(PmsProduct::getStock, 2)
                .setSql("stock = stock - 2")
                .setSql("version = version + 1"));

        assertFalse(staleUpdateSucceeded);
        PmsProduct updatedProduct = productMapper.selectById(product.getId());
        assertEquals(8, updatedProduct.getStock());
        assertEquals(1, updatedProduct.getVersion());
    }

    private SysUser createUser() {
        SysUser user = new SysUser();
        user.setUsername("order_test_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPassword("test-only");
        user.setNickname("order test");
        user.setRole("user");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private UmsAddress createAddress(Long userId) {
        UmsAddress address = new UmsAddress();
        address.setUserId(userId);
        address.setName("测试用户");
        address.setPhone("13800138000");
        address.setProvince("四川省");
        address.setCity("成都市");
        address.setRegion("郫都区");
        address.setDetailAddress("测试路1号");
        address.setIsDefault(1);
        addressMapper.insert(address);
        return address;
    }

    private PmsCategory createCategory() {
        PmsCategory category = new PmsCategory();
        category.setName("订单测试分类");
        category.setSort(0);
        category.setStatus(1);
        categoryMapper.insert(category);
        return category;
    }

    private PmsProduct createProduct(Long categoryId) {
        PmsProduct product = new PmsProduct();
        product.setCategoryId(categoryId);
        product.setName("订单测试商品");
        product.setPrice(new BigDecimal("19.90"));
        product.setStock(10);
        product.setVersion(0);
        product.setCoverImg("/images/order-test.png");
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
}
