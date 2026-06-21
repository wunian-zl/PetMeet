package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.PmsCategory;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.PmsProductMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.PmsCategoryService;
import org.petmeet.service.PmsProductService;
import org.petmeet.vo.ProductDetailVO;
import org.petmeet.vo.ProductListVO;
import org.petmeet.vo.ProductReviewVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmsProductServiceImpl extends ServiceImpl<PmsProductMapper, PmsProduct> implements PmsProductService {

    private static final int REVIEW_PREVIEW_LIMIT = 20;

    private final PmsCategoryService pmsCategoryService;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final SysUserMapper userMapper;

    /**
     * 商品分页列表
     */
    @Override
    public Page<ProductListVO> pageList(Integer pageNum,
                                        Integer pageSize, List<Long>
            categoryIds, String keyword, Integer recentDays) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PmsProduct::getStatus, PmsProduct.STATUS_ON_SHELF);
        if (categoryIds != null && !categoryIds.isEmpty()) {
            wrapper.in(PmsProduct::getCategoryId, categoryIds); }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w ->
                    w.like(PmsProduct::getName, keyword)
                    .or().like(PmsProduct::getSubTitle, keyword)); }
        if (recentDays != null && recentDays > 0) {
            wrapper.ge(PmsProduct::getCreateTime,
                    LocalDateTime.now().minusDays(recentDays)); }
        wrapper.orderByDesc(PmsProduct::getCreateTime);
        Page<PmsProduct> page = new Page<>(pageNum, pageSize);
        this.page(page, wrapper);
        Page<ProductListVO> voPage = new Page<>(page.getCurrent(),
                page.getSize(), page.getTotal());
        List<ProductListVO> voList = page.getRecords().stream()
                .map(p -> {
            ProductListVO vo = new ProductListVO();
            BeanUtil.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage; }
    /**
     * 商品详情
     */
    @Override
    public ProductDetailVO getDetail(Long productId) {
        PmsProduct product = this.getById(productId);
        if (product == null) {
            throw AppException.notFound("商品不存在");
        }

        // 先把商品主信息拷贝到详情对象
        ProductDetailVO vo = new ProductDetailVO();
        BeanUtil.copyProperties(product, vo);

        // 主图组（可选）
        if (StrUtil.isNotBlank(product.getCoverImgs())) {
            vo.setCoverImgs(JSON.parseArray(product.getCoverImgs(), String.class));
        } else if (StrUtil.isNotBlank(product.getCoverImg())) {
            vo.setCoverImgs(Collections.singletonList(product.getCoverImg()));
        } else {
            vo.setCoverImgs(Collections.emptyList());
        }

        if (StrUtil.isNotBlank(product.getDetailImgs())) {
            vo.setDetailImgs(JSON.parseArray(product.getDetailImgs(), String.class));
        } else {
            vo.setDetailImgs(Collections.emptyList());
        }

        PmsCategory category = pmsCategoryService.getById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        fillReviewData(vo, productId);
        return vo;
    }

    /**
     * 扣减库存
     */
    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        PmsProduct product = this.getById(productId);
        if (product == null) {
            throw AppException.notFound("商品不存在");
        }
        if (product.getStock() < quantity) {
            throw AppException.badRequest("库存不足");
        }

        LambdaUpdateWrapper<PmsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PmsProduct::getId, productId)
                .eq(PmsProduct::getVersion, product.getVersion())
                .ge(PmsProduct::getStock, quantity)
                .setSql("stock = stock - " + quantity)
                .setSql("version = version + 1");
        return this.update(wrapper);
    }

    /**
     * 填充评价数据
     */
    private void fillReviewData(ProductDetailVO vo, Long productId) {
        List<OmsOrderItem> productItems = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                .select(OmsOrderItem::getOrderId, OmsOrderItem::getQuantity)
                .eq(OmsOrderItem::getProductId, productId));
        if (productItems == null || productItems.isEmpty()) {
            setEmptyReviewData(vo);
            return;
        }

        Set<Long> orderIds = new HashSet<>();
        Map<Long, Integer> quantityMap = new HashMap<>();
        for (OmsOrderItem item : productItems) {
            if (item.getOrderId() == null) {
                continue;
            }
            orderIds.add(item.getOrderId());
            quantityMap.merge(item.getOrderId(), Math.max(1, item.getQuantity() == null ? 1 : item.getQuantity()), Integer::sum);
        }
        if (orderIds.isEmpty()) {
            setEmptyReviewData(vo);
            return;
        }

        List<OmsOrder> reviewedOrders = orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                .in(OmsOrder::getId, orderIds)
                .eq(OmsOrder::getReviewStatus, OmsOrder.REVIEW_DONE)
                .isNotNull(OmsOrder::getReviewScore)
                .orderByDesc(OmsOrder::getReviewTime)
                .orderByDesc(OmsOrder::getId));
        if (reviewedOrders == null || reviewedOrders.isEmpty()) {
            setEmptyReviewData(vo);
            return;
        }

        vo.setReviewCount(reviewedOrders.size());
        double avgScore = reviewedOrders.stream()
                .map(OmsOrder::getReviewScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        vo.setReviewAvgScore(Math.round(avgScore * 10.0) / 10.0);

        Set<Long> userIds = reviewedOrders.stream()
                .map(OmsOrder::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<ProductReviewVO> reviewList = reviewedOrders.stream()
                .limit(REVIEW_PREVIEW_LIMIT)
                .map(order -> {
                    ProductReviewVO reviewVO = new ProductReviewVO();
                    reviewVO.setOrderId(order.getId());
                    reviewVO.setOrderSn(order.getOrderSn());
                    reviewVO.setScore(order.getReviewScore());
                    reviewVO.setContent(order.getReviewContent());
                    reviewVO.setReviewTime(order.getReviewTime());
                    reviewVO.setUserId(order.getUserId());
                    reviewVO.setQuantity(quantityMap.getOrDefault(order.getId(), 1));
                    SysUser user = userMap.get(order.getUserId());
                    if (user != null) {
                        reviewVO.setUsername(user.getUsername());
                        reviewVO.setNickname(user.getNickname());
                        reviewVO.setUserAvatar(user.getAvatar());
                    }
                    return reviewVO;
                })
                .toList();
        vo.setReviewList(reviewList);
    }

    /**
     * 设置空评价数据
     */
    private void setEmptyReviewData(ProductDetailVO vo) {
        vo.setReviewCount(0);
        vo.setReviewAvgScore(0D);
        vo.setReviewList(Collections.emptyList());
    }
}
