package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "订单评价请求")
public class OrderReviewDTO {

    @Schema(description = "评分(1-5)")
    @NotNull(message = "请给出评分")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer score;

    @Schema(description = "评价内容")
    @Size(max = 500, message = "评价内容过长")
    private String content;
}
