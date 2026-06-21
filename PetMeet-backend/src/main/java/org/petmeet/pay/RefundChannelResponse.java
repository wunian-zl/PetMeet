package org.petmeet.pay;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundChannelResponse {
    private boolean success;
    private String tradeNo;
    private String errorMsg;
}
