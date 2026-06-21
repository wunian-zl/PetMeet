package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oms_refund_log")
public class OmsRefundLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundSn;
    private Long orderId;
    private String orderSn;
    private Long payLogId;
    private Long afterSaleId;
    private Long userId;
    private Integer payType;
    private BigDecimal refundAmount;
    private String refundReason;
    private Integer refundStatus;
    private String tradeNo;
    private LocalDateTime refundTime;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
