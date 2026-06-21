package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "My complaint item")
public class MyComplaintVO {

    private Long id;

    private Long noteId;

    private String targetType;

    private Long commentId;

    private String commentContent;

    private String commentAuthorName;

    private Long parentId;

    private String noteTitle;

    private String noteCover;

    private String reason;

    private String content;

    private List<String> evidenceImages;

    /**
     * 0 表示待处理，1 表示已处理，2 表示已驳回。
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime handleTime;

    private String handleRemark;

    /**
     * 0 表示未反馈，1 表示满意，2 表示不满意。
     */
    private Integer feedbackStatus;

    private String feedbackContent;

    private LocalDateTime feedbackTime;
}
