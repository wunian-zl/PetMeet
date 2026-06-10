package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.entity.PmsProduct;
import org.petmeet.vo.ProductDetailVO;
import org.petmeet.vo.ProductListVO;

import java.util.List;

public interface PmsProductService extends IService<PmsProduct> {
    Page<ProductListVO> pageList(Integer pageNum, Integer pageSize, List<Long> categoryIds, String keyword, Integer recentDays);

    ProductDetailVO getDetail(Long productId);

    boolean deductStock(Long productId, Integer quantity);
}
