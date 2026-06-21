package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建支付请求")
public class PayCreateDTO {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderSn;

    @Schema(description = "支付方式:ALIPAY或WECHAT_MOCK")
    private String payType;

    @Schema(description = "支付模式:v1固定QR_CODE")
    private String payMode;

    @Schema(description = "是否强制重新生成支付流水")
    private Boolean forceRefresh;
}
