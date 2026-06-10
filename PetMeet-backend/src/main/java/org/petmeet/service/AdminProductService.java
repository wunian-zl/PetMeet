package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.petmeet.dto.AdminProductSaveDTO;
import org.petmeet.vo.AdminProductDetailVO;
import org.petmeet.vo.AdminProductVO;

import java.util.List;

/**
 * 管理端商品服务接口
 */
public interface AdminProductService {

    Page<AdminProductVO> pageList(Integer pageNum, Integer pageSize, Long categoryId, Integer status, String keyword);

    AdminProductDetailVO getDetail(Long id);

    Long createProduct(AdminProductSaveDTO product);

    void updateProduct(AdminProductSaveDTO product);

    void changeStatus(Long id, Integer status);

    void deleteProduct(Long id);

    void batchAction(String action, List<Long> ids);

}
