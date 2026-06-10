package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.petmeet.vo.AdminOrderVO;

import java.util.List;
import java.util.Map;

/**
 * 管理端订单服务接口
 */
public interface AdminOrderService {

    Page<AdminOrderVO> pageList(Integer pageNum, Integer pageSize, Integer status, String orderNo, String startTime,
            String endTime);

    AdminOrderVO getDetail(Long id);

    void ship(Long id, String company, String trackingNo);

    void refund(Long id, Map<String, Object> refundInfo);

    void cancel(Long id);

    void updateAddress(Long id, Map<String, String> addressInfo);

    String export(Integer status, String startTime, String endTime);

    void softDelete(Long id);

    void batchSoftDelete(List<Long> ids);
}
