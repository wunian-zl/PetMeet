package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.dto.AdminProductSaveDTO;
import org.petmeet.entity.PmsCategory;
import org.petmeet.entity.PmsProduct;
import org.petmeet.mapper.PmsProductMapper;
import org.petmeet.mapper.PmsCategoryMapper;
import org.petmeet.service.AdminProductService;
import org.petmeet.vo.AdminProductDetailVO;
import org.petmeet.vo.AdminProductVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final PmsProductMapper pmsProductMapper;
    private final PmsCategoryMapper pmsCategoryMapper;

    /**
     * 商品列表
     */
    @Override
    public Page<AdminProductVO> pageList(Integer pageNum, Integer pageSize, Long categoryId, Integer status,
            String keyword) {
        Page<PmsProduct> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            wrapper.eq(PmsProduct::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(PmsProduct::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            Long productId = parseProductIdKeyword(kw);
            wrapper.and(w -> {
                w.like(PmsProduct::getName, kw);
                if (productId != null) {
                    w.or().eq(PmsProduct::getId, productId);
                }
            });
        }
        wrapper.orderByDesc(PmsProduct::getCreateTime);

        // 查询后台商品分页
        Page<PmsProduct> productPage = pmsProductMapper.selectPage(page, wrapper);

        // 转成后台展示对象
        Page<AdminProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(),
                productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 商品详情
     */
    @Override
    public AdminProductDetailVO getDetail(Long id) {
        // 查询商品详情
        PmsProduct product = pmsProductMapper.selectById(id);
        if (product == null) {
            throw AppException.notFound("商品不存在");
        }
        AdminProductDetailVO vo = new AdminProductDetailVO();
        BeanUtil.copyProperties(toVO(product), vo);
        return vo;
    }

    /**
     * 新增商品
     */
    @Override
    public Long createProduct(AdminProductSaveDTO dto) {
        // 组装商品数据
        PmsProduct product = new PmsProduct();
        BeanUtil.copyProperties(dto, product);
        normalizeCoverImgs(product);
        product.setCreateTime(LocalDateTime.now());
        if (product.getStatus() == null) {
            product.setStatus(PmsProduct.STATUS_OFF_SHELF);
        }
        if (product.getSales() == null) {
            product.setSales(0);
        }
        if (product.getViews() == null) {
            product.setViews(0);
        }
        pmsProductMapper.insert(product);
        return product.getId();
    }

    /**
     * 修改商品
     */
    @Override
    public void updateProduct(AdminProductSaveDTO dto) {
        // 查询原商品
        PmsProduct existing = pmsProductMapper.selectById(dto.getId());
        if (existing == null) {
            throw AppException.notFound("商品不存在");
        }
        PmsProduct product = new PmsProduct();
        BeanUtil.copyProperties(dto, product);
        // 关键：若本次更新未传 coverImgs，则保留库中已有的主图组，避免被覆盖成单张
        if (StrUtil.isBlank(dto.getCoverImgs())) {
            product.setCoverImgs(existing.getCoverImgs());
            // 同时确保 coverImg 不被意外置空
            if (StrUtil.isBlank(dto.getCoverImg())) {
                product.setCoverImg(existing.getCoverImg());
            }
        }
        normalizeCoverImgs(product);
        pmsProductMapper.updateById(product);
    }

    /**
     * 修改商品状态
     */
    @Override
    public void changeStatus(Long id, Integer status) {
        PmsProduct product = pmsProductMapper.selectById(id);
        if (product == null) {
            throw AppException.notFound("商品不存在");
        }
        product.setStatus(status);
        pmsProductMapper.updateById(product);
    }

    /**
     * 删除商品
     */
    @Override
    public void deleteProduct(Long id) {
        PmsProduct product = pmsProductMapper.selectById(id);
        if (product == null) {
            throw AppException.notFound("商品不存在");
        }
        pmsProductMapper.deleteById(id);
    }

    /**
     * 批量操作商品
     */
    @Override
    public void batchAction(String action, List<Long> ids) {
        // 逐个执行批量动作
        for (Long id : ids) {
            switch (action) {
                case "online" -> changeStatus(id, 1);
                case "offline" -> changeStatus(id, 0);
                case "delete" -> pmsProductMapper.deleteById(id);
            }
        }
    }

    /**
     * 转换后台商品数据
     */
    private AdminProductVO toVO(PmsProduct product) {
        AdminProductVO vo = new AdminProductVO();
        BeanUtil.copyProperties(product, vo);

        // 兼容字段映射
        vo.setSubTitle(product.getSubTitle());
        vo.setCoverImg(product.getCoverImg());
        vo.setCoverImgs(product.getCoverImgs());
        vo.setDetailImgs(product.getDetailImgs());
        vo.setCover(product.getCoverImg());
        vo.setImages(product.getDetailImgs());

        // 查询分类名称
        if (product.getCategoryId() != null) {
            PmsCategory category = pmsCategoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        return vo;
    }

    /**
     * 解析商品关键词中的商品ID
     */
    private Long parseProductIdKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return null;
        }
        String text = keyword.trim();
        if (StrUtil.startWithIgnoreCase(text, "ID:") || StrUtil.startWithIgnoreCase(text, "ID：")) {
            text = text.substring(3).trim();
        }
        if (!text.matches("^\\d+$")) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 规范商品主图数据
     */
    private void normalizeCoverImgs(PmsProduct product) {
        if (product == null) return;
        if (StrUtil.isBlank(product.getCoverImgs())) {
            // 没传主图组：如果传了 coverImg，则自动补成单张数组
            if (StrUtil.isNotBlank(product.getCoverImg())) {
                product.setCoverImgs(JSON.toJSONString(List.of(product.getCoverImg())));
            }
            return;
        }
        try {
            List<String> imgs = JSON.parseArray(product.getCoverImgs(), String.class);
            if (imgs == null || imgs.isEmpty()) {
                // 空数组：回退为 coverImg
                if (StrUtil.isNotBlank(product.getCoverImg())) {
                    product.setCoverImgs(JSON.toJSONString(List.of(product.getCoverImg())));
                } else {
                    product.setCoverImgs(null);
                }
                return;
            }
            // 限制最多5张
            if (imgs.size() > 5) {
                imgs = imgs.subList(0, 5);
                product.setCoverImgs(JSON.toJSONString(imgs));
            }
            product.setCoverImg(imgs.get(0));
        } catch (Exception e) {
            // 解析失败：回退为 coverImg 单张
            if (StrUtil.isNotBlank(product.getCoverImg())) {
                product.setCoverImgs(JSON.toJSONString(List.of(product.getCoverImg())));
            } else {
                product.setCoverImgs(null);
            }
        }
    }
}
