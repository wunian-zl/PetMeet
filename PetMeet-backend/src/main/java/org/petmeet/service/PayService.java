package org.petmeet.service;

import org.petmeet.dto.PayCreateDTO;
import org.petmeet.entity.OmsRefundLog;
import org.petmeet.vo.PayResponseVO;
import org.petmeet.vo.PayStatusVO;

import java.util.Map;

public interface PayService {
    PayResponseVO createPay(PayCreateDTO dto);

    PayStatusVO queryPayStatus(String paySn, boolean syncChannel);

    void mockConfirm(String paySn);

    String handleAlipayNotify(Map<String, String> params);

    OmsRefundLog refundOrder(Long orderId, Long afterSaleId, String reason);
}
