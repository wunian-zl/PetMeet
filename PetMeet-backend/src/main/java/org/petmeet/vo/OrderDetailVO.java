package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Order detail")
public class OrderDetailVO {
    private Long id;
    private String orderSn;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusDesc;
    private Integer reviewStatus;
    private Integer reviewScore;
    private String reviewContent;
    private LocalDateTime reviewTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    /**
     * 待付款订单的支付截止时间（createTime + 超时时间窗口）。
     */
    private LocalDateTime payExpireTime;
    private String shipCompany;
    private String trackingNo;
    private LocalDateTime shipTime;
    private List<OrderItemVO> items;
}
