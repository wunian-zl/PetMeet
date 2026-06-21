package org.petmeet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayStatusVO {
    private String paySn;
    private String orderSn;
    private String status;
    private LocalDateTime payTime;
}
