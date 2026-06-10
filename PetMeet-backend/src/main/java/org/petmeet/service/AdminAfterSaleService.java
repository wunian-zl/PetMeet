package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.petmeet.vo.AdminAfterSaleVO;

import java.util.List;

public interface AdminAfterSaleService {
    Page<AdminAfterSaleVO> pageList(Integer pageNum, Integer pageSize, Integer status, String keyword);

    void updateStatus(Long id, Integer status, String remark);

    void softDelete(Long id);

    void batchSoftDelete(List<Long> ids);
}
