package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交订单请求对象
 *
 * @author zjx
 */
@Data
@Schema(description = "提交订单请求参数")
public class OrderSubmitDTO {

    @NotEmpty(message = "请选择要结算的商品")
    @Schema(description = "购物车项ID列表", example = "[1, 2, 3]")
    private List<Long> cartItemIds;

    @NotNull(message = "请选择收货地址")
    @Schema(description = "收货地址ID", example = "1")
    private Long addressId;

    @Schema(description = "订单备注")
    private String remark;

    @Schema(description = "用户选择的支付方式:ALIPAY或WECHAT_MOCK")
    private String payType;
}
