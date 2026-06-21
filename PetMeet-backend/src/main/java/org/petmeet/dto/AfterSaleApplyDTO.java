package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "After-sale apply request")
public class AfterSaleApplyDTO {

    @Schema(description = "Order id")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "Order item id")
    @NotNull(message = "订单项ID不能为空")
    private Long orderItemId;

    @Schema(description = "Type: 0 refund-only, 1 return-refund, 2 exchange")
    @NotNull(message = "售后类型不能为空")
    private Integer type;

    @Schema(description = "Reason")
    @Size(max = 100, message = "原因不能超过100个字符")
    private String reason;

    @Schema(description = "Description")
    @Size(max = 500, message = "说明不能超过500个字符")
    private String description;

    @Schema(description = "Evidence image urls")
    private List<String> evidenceImages;
}
