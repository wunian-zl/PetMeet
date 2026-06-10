package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 发布笔记请求参数
 */
@Data
@Schema(description = "Note publish request")
public class NotePublishDTO {

    @NotBlank(message = "title is required")
    @Schema(description = "Note title", example = "My pet note")
    private String title;

    @NotBlank(message = "content is required")
    @Schema(description = "Note content")
    private String content;

    @Schema(description = "Category", example = "cat")
    private String category;

    @Schema(description = "Tags", example = "[\"food\",\"review\"]")
    private List<String> tags;

    @Schema(description = "Cover image")
    private String coverImg;

    @Schema(description = "Image list")
    private List<String> images;

    @Schema(description = "Type: image/video", example = "image")
    private String type;

    @Schema(description = "Video url", example = "/images/note/video/2026/01/xxx.mp4")
    private String videoUrl;

    @Schema(description = "Related product ids", example = "[1,2,3]")
    private List<Long> productIds;
}
