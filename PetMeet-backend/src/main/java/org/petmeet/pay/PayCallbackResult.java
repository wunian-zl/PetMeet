package org.petmeet.pay;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayCallbackResult {
    private boolean verified;
    private String appId;
    private String paySn;
    private String tradeNo;
    private String tradeStatus;
    private BigDecimal totalAmount;
    private String errorMsg;
}
