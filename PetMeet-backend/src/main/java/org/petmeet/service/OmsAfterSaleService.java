package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.dto.AfterSaleReturnLogisticsDTO;
import org.petmeet.dto.AfterSaleApplyDTO;
import org.petmeet.entity.OmsAfterSale;
import org.petmeet.vo.AfterSaleVO;

public interface OmsAfterSaleService extends IService<OmsAfterSale> {
    Long apply(AfterSaleApplyDTO dto);

    Page<AfterSaleVO> pageMy(Integer pageNum, Integer pageSize, Integer status);

    void cancel(Long id);

    void complete(Long id);

    void deleteMy(Long id);

    void submitReturnLogistics(Long id, AfterSaleReturnLogisticsDTO dto);
}
