package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.dto.OrderReviewDTO;
import org.petmeet.dto.OrderSubmitDTO;
import org.petmeet.entity.OmsOrder;
import org.petmeet.vo.OrderDetailVO;

import java.util.List;

public interface OmsOrderService extends IService<OmsOrder> {
    Long submitOrder(OrderSubmitDTO dto);

    OrderDetailVO getOrderDetail(Long orderId);

    Page<OrderDetailVO> pageMyOrders(Integer pageNum, Integer pageSize, Integer status, Integer reviewStatus);

    void pay(Long orderId);

    void cancel(Long orderId);

    void confirmReceipt(Long orderId);

    void review(Long orderId, OrderReviewDTO dto);

    void deleteReview(Long orderId);

    void deleteMyOrder(Long orderId);

    void batchDeleteMyOrders(List<Long> orderIds);
}
