package org.petmeet.pay;

import lombok.Builder;
import lombok.Data;
import org.petmeet.enums.PayTypeEnum;

import java.math.BigDecimal;

@Data
@Builder
public class RefundChannelRequest {
    private String paySn;
    private String refundSn;
    private String tradeNo;
    private String orderSn;
    private BigDecimal refundAmount;
    private String reason;
    private PayTypeEnum payType;
}
