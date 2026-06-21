package org.petmeet.pay;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayQueryResult {
    private boolean success;
    private boolean paid;
    private boolean closed;
    private String tradeNo;
    private String tradeStatus;
    private BigDecimal totalAmount;
    private String rawContent;
    private String errorMsg;
}
