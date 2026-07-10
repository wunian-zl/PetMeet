package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Admin order view")
public class AdminOrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private String nickname;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusDesc;
    private String payType;
    private String paySn;
    private String tradeNo;
    private BigDecimal refundAmount;
    private String address;
    private String receiver;
    private String phone;
    private String remark;
    private String shipCompany;
    private String trackingNo;
    private List<OrderItemVO> items;
    private RefundVO refund;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;

    @Data
    public static class OrderItemVO {
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
    }

    @Data
    public static class RefundVO {
        private Long afterSaleId;
        private String refundSn;
        private String reason;
        private String description;
        private List<String> evidenceImages;
        private BigDecimal afterSaleRefundAmount;
        private String returnCompany;
        private String returnTrackingNo;
        private String exchangeCompany;
        private String exchangeTrackingNo;
        private Integer status;
        private String statusDesc;
        private BigDecimal refundAmount;
        private Integer refundStatus;
        private String refundStatusDesc;
        private String refundTradeNo;
        private LocalDateTime refundTime;
    }
}
