package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Complaint feedback")
public class ComplaintFeedbackDTO {

    /**
     * 1 表示满意，2 表示不满意。
     */
    @NotNull(message = "反馈状态不能为空")
    @Min(value = 1, message = "反馈状态只能为1或2")
    @Max(value = 2, message = "反馈状态只能为1或2")
    @Schema(description = "1=satisfied,2=unsatisfied", example = "1")
    private Integer feedbackStatus;

    @Schema(description = "Feedback content (optional)")
    private String content;
}
