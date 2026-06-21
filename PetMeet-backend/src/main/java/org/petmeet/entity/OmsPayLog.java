package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oms_pay_log")
public class OmsPayLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String paySn;
    private Long orderId;
    private String orderSn;
    private Long userId;
    private Integer payType;
    private Integer payMode;
    private BigDecimal payAmount;
    private Integer payStatus;
    private String tradeNo;
    private String qrCodeUrl;
    private String payPageUrl;
    private LocalDateTime expireTime;
    private LocalDateTime payTime;
    private LocalDateTime callbackTime;
    private String callbackContent;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
