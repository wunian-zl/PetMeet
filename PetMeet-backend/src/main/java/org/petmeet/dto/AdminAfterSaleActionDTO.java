package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Admin after-sale action request")
public class AdminAfterSaleActionDTO {

    @Schema(description = "Handle remark")
    @Size(max = 500, message = "处理备注不能超过500个字符")
    private String remark;

    @Schema(description = "Return address for buyer")
    @Size(max = 500, message = "退货地址不能超过500个字符")
    private String returnAddress;

    @Schema(description = "Exchange logistics company")
    @Size(max = 100, message = "物流公司不能超过100个字符")
    private String exchangeCompany;

    @Schema(description = "Exchange tracking number")
    @Size(max = 100, message = "物流单号不能超过100个字符")
    private String exchangeTrackingNo;
}
