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
    @NotNull(message = "orderId is required")
    private Long orderId;

    @Schema(description = "Order item id")
    @NotNull(message = "orderItemId is required")
    private Long orderItemId;

    @Schema(description = "Type: 0 refund-only, 1 return-refund, 2 exchange")
    @NotNull(message = "type is required")
    private Integer type;

    @Schema(description = "Reason")
    @Size(max = 100, message = "reason is too long")
    private String reason;

    @Schema(description = "Description")
    @Size(max = 500, message = "description is too long")
    private String description;

    @Schema(description = "Evidence image urls")
    private List<String> evidenceImages;
}
