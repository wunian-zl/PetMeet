package org.petmeet.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.dto.CartAddDTO;
import org.petmeet.entity.OmsCartItem;
import org.petmeet.entity.PmsProduct;
import org.petmeet.mapper.OmsCartItemMapper;
import org.petmeet.service.OmsCartItemService;
import org.petmeet.service.PmsProductService;
import org.petmeet.vo.CartItemVO;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmsCartItemServiceImpl extends ServiceImpl<OmsCartItemMapper, OmsCartItem> implements OmsCartItemService {

    private final PmsProductService pmsProductService;

    /**
     * 添加到购物车
     */
    @Override
    public void addToCart(CartAddDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 先校验商品状态和库存
        PmsProduct product = pmsProductService.
                getById(dto.getProductId());
        if (product == null)
            throw new RuntimeException("商品不存在");
        if (product.getStatus() != 1)
            throw new RuntimeException("商品已下架");
        if (product.getStock() < dto.getQuantity())
            throw new RuntimeException("商品库存不足");
        // 已经在购物车里的商品要累计数量
        OmsCartItem existing = this.baseMapper.
                selectByUserAndProductIgnoreDeleted(userId, dto.getProductId());
        int newQty = dto.getQuantity();
        if (existing != null && !Integer.valueOf(1).equals(existing.getIsDeleted())) {
            newQty = existing.getQuantity() + dto.getQuantity(); }
        if (newQty > product.getStock())
            throw new RuntimeException("超出库存限制");
        // 插入或更新购物车项
        this.baseMapper.upsertCartItem(userId, dto.getProductId(),
                dto.getQuantity(), LocalDateTime.now());
    }

    /**
     * 修改购物车数量
     */
    @Override
    public void updateQuantity(Long cartItemId, Integer quantity) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsCartItem item = this.getById(cartItemId);
        if (item == null || !item.getUserId().equals(userId))
            throw new RuntimeException("购物车项不存在");

        // 数量小于等于 0 时直接删除购物车项
        if (quantity <= 0) {
            this.removeById(cartItemId);
        } else {
            // 更新前再次校验库存
            PmsProduct product = pmsProductService.getById(item.getProductId());
            if (product != null && quantity > product.getStock()) {
                throw new RuntimeException("超出库存限制");
            }
            item.setQuantity(quantity);
            this.updateById(item);
        }
    }

    /**
     * 删除购物车项
     */
    @Override
    public void deleteCartItem(Long cartItemId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsCartItem item = this.getById(cartItemId);
        if (item == null || !item.getUserId().equals(userId))
            throw new RuntimeException("购物车项不存在");
        this.removeById(cartItemId);
    }

    /**
     * 批量删除购物车项
     */
    @Override
    public void batchDelete(List<Long> cartItemIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<OmsCartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsCartItem::getUserId, userId).in(OmsCartItem::getId, cartItemIds);
        this.remove(wrapper);
    }

    /**
     * 当前用户购物车列表
     */
    @Override
    public List<CartItemVO> listByCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<OmsCartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsCartItem::getUserId, userId).orderByDesc(OmsCartItem::getCreateTime);
        List<OmsCartItem> items = this.list(wrapper);

        if (items.isEmpty())
            return new ArrayList<>();

        Set<Long> productIds = items.stream().map(OmsCartItem::getProductId).collect(Collectors.toSet());
        Map<Long, PmsProduct> productMap = pmsProductService.listByIds(productIds).stream()
                .collect(Collectors.toMap(PmsProduct::getId, p -> p));

        return items.stream().map(item -> {
            CartItemVO vo = new CartItemVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setQuantity(item.getQuantity());
            vo.setCreateTime(item.getCreateTime());

            // 把商品快照信息补到返回结果里
            PmsProduct p = productMap.get(item.getProductId());
            if (p != null) {
                vo.setProductName(p.getName());
                vo.setProductImg(p.getCoverImg());
                vo.setPrice(p.getPrice());
                vo.setStock(p.getStock());
                vo.setProductStatus(p.getStatus());
                if (vo.getPrice() != null) {
                    vo.setSubtotal(vo.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 购物车数量
     */
    @Override
    public Integer getCartCount() {
        if (!StpUtil.isLogin()) {
            return 0;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<OmsCartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsCartItem::getUserId, userId);
        return (int) this.count(wrapper);
    }

    /**
     * 移除购物车商品
     */
    @Override
    public void removeFromCart(Long cartItemId) {
        deleteCartItem(cartItemId);
    }

    /**
     * 获取购物车列表
     */
    @Override
    public List<CartItemVO> getCartList() {
        return listByCurrentUser();
    }

    /**
     * 清空购物车
     */
    @Override
    public void clearCart() {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<OmsCartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsCartItem::getUserId, userId);
        this.remove(wrapper);
    }

    /**
     * 更新选中状态
     */
    @Override
    public void updateSelected(Long cartItemId, Boolean selected) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsCartItem item = this.getById(cartItemId);
        if (item == null || !item.getUserId().equals(userId))
            throw new RuntimeException("购物车项不存在");
        item.setSelected(selected);
        this.updateById(item);
    }

    /**
     * 全选或取消全选
     */
    @Override
    public void selectAll(Boolean selected) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<OmsCartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsCartItem::getUserId, userId);
        List<OmsCartItem> items = this.list(wrapper);
        for (OmsCartItem item : items) {
            item.setSelected(selected);
        }
        this.updateBatchById(items);
    }
}
