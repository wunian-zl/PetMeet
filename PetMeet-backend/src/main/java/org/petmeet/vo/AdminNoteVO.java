package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端笔记视图对象
 */
@Data
@Schema(description = "Admin note")
public class AdminNoteVO {

    @Schema(description = "Note id")
    private Long id;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Category")
    private String category;

    @Schema(description = "Tags (comma separated)")
    private String tags;

    @Schema(description = "Cover image")
    private String cover;

    @Schema(description = "Type: image/video")
    private String type;

    @Schema(description = "Video url")
    private String videoUrl;

    @Schema(description = "Content")
    private String content;

    @Schema(description = "Author user id")
    private Long userId;

    @Schema(description = "Author username")
    private String username;

    @Schema(description = "Author nickname")
    private String nickname;

    @Schema(description = "Author avatar")
    private String userAvatar;

    @Schema(description = "Status: 0-pending,1-approved,2-shielded,3-rejected")
    private Integer status;

    @Schema(description = "Sticky")
    private Boolean isSticky;

    @Schema(description = "Recommended")
    private Boolean isRecommended;

    @Schema(description = "Like count")
    private Integer likeCount;

    @Schema(description = "Collect count")
    private Integer collectCount;

    @Schema(description = "Comment count")
    private Integer commentCount;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Audit time")
    private LocalDateTime auditTime;

    @Schema(description = "Audit operator user id")
    private Long auditUserId;

    @Schema(description = "Audit operator name")
    private String auditUserName;

    @Schema(description = "Reject reason")
    private String rejectReason;
}
