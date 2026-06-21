package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Complaint request")
public class ComplaintDTO {

    @NotNull(message = "笔记ID不能为空")
    @Schema(description = "Note id")
    private Long noteId;

    @Schema(description = "Target type: note/comment")
    private String targetType;

    @Schema(description = "Comment id when targetType=comment")
    private Long commentId;

    @Schema(description = "Parent complaint id for re-complaint")
    private Long parentId;

    @NotBlank(message = "原因不能为空")
    @Schema(description = "Reason")
    private String reason;

    @Schema(description = "Detail")
    private String content;

    @Schema(description = "Evidence image urls")
    private List<String> evidenceImages;
}
