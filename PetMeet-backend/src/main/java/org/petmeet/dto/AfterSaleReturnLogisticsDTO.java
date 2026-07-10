package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "After-sale return logistics request")
public class AfterSaleReturnLogisticsDTO {

    @NotBlank(message = "退货物流公司不能为空")
    @Size(max = 100, message = "物流公司不能超过100个字符")
    private String company;

    @NotBlank(message = "退货物流单号不能为空")
    @Size(max = 100, message = "物流单号不能超过100个字符")
    private String trackingNo;
}
