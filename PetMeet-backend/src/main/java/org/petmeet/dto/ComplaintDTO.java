package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Note complaint request")
public class ComplaintDTO {

    @NotNull(message = "noteId is required")
    @Schema(description = "Note id")
    private Long noteId;

    @Schema(description = "Parent complaint id (for re-complaint)")
    private Long parentId;

    @NotBlank(message = "reason is required")
    @Schema(description = "Reason", example = "侵权")
    private String reason;

    @Schema(description = "Detail")
    private String content;
}
