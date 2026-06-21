package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Comment create request")
public class CommentCreateDTO {

    @NotNull(message = "笔记ID不能为空")
    private Long noteId;

    @Schema(description = "Root comment id when replying")
    private Long parentId;

    @Schema(description = "The exact comment being replied to")
    private Long replyToId;

    @NotBlank(message = "内容不能为空")
    @Size(max = 500, message = "内容不能超过500个字符")
    private String content;
}
