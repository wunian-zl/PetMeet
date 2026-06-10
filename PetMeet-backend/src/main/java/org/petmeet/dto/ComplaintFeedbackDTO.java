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
    @NotNull(message = "feedbackStatus is required")
    @Min(value = 1, message = "feedbackStatus must be 1 or 2")
    @Max(value = 2, message = "feedbackStatus must be 1 or 2")
    @Schema(description = "1=satisfied,2=unsatisfied", example = "1")
    private Integer feedbackStatus;

    @Schema(description = "Feedback content (optional)")
    private String content;
}
