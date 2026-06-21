package org.petmeet.pay;

import lombok.Builder;
import lombok.Data;
import org.petmeet.enums.PayModeEnum;
import org.petmeet.enums.PayTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PayChannelRequest {
    private String paySn;
    private String orderSn;
    private Long orderId;
    private Long userId;
    private PayTypeEnum payType;
    private PayModeEnum payMode;
    private BigDecimal amount;
    private String subject;
    private LocalDateTime expireTime;
}
