package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.petmeet.dto.AdminAfterSaleActionDTO;
import org.petmeet.vo.AdminAfterSaleVO;

import java.util.List;

public interface AdminAfterSaleService {
    Page<AdminAfterSaleVO> pageList(Integer pageNum, Integer pageSize, Integer status, Integer type, String keyword);

    AdminAfterSaleVO detail(Long id);

    void updateStatus(Long id, Integer status, String remark);

    void approveRefund(Long id, AdminAfterSaleActionDTO dto);

    void approveReturn(Long id, AdminAfterSaleActionDTO dto);

    void confirmReturnRefund(Long id, AdminAfterSaleActionDTO dto);

    void confirmReturnExchange(Long id, AdminAfterSaleActionDTO dto);

    void shipExchange(Long id, AdminAfterSaleActionDTO dto);

    void reject(Long id, AdminAfterSaleActionDTO dto);

    void softDelete(Long id);

    void batchSoftDelete(List<Long> ids);
}
