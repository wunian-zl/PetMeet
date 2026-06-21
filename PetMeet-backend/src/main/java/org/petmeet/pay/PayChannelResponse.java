package org.petmeet.pay;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayChannelResponse {
    private boolean success;
    private String qrCodeUrl;
    private String tradeNo;
    private String errorMsg;
}
