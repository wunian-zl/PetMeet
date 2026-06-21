package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Admin complaint view")
public class AdminComplaintVO {
    private Long id;
    private Long noteId;
    private String targetType;
    private Long commentId;
    private String commentContent;
    private Long commentAuthorId;
    private String commentAuthorName;
    private Long commentParentId;
    private Long commentReplyToId;
    private LocalDateTime commentCreateTime;
    private Boolean commentDeleted;
    private String parentCommentContent;
    private String parentCommentAuthorName;
    private String replyToContent;
    private String replyToAuthorName;
    private Long parentId;
    private String noteTitle;
    private Long noteAuthorId;
    private String noteAuthorName;

    private Long reporterId;
    private String reporterName;
    private String reporterAvatar;

    private String reason;
    private String content;
    private List<String> evidenceImages;
    private Integer status;

    private String handleRemark;

    /**
     * 0 表示未反馈，1 表示满意，2 表示不满意。
     */
    private Integer feedbackStatus;
    private String feedbackContent;
    private LocalDateTime feedbackTime;

    private LocalDateTime createTime;
    private LocalDateTime handleTime;
    private Long handlerId;
}
