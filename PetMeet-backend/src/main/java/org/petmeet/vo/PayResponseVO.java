package org.petmeet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayResponseVO {
    private String paySn;
    private String orderSn;
    private String payType;
    private String payStatus;
    private BigDecimal amount;
    private String qrCodeUrl;
    private LocalDateTime expireTime;
    private Boolean sandbox;
}
