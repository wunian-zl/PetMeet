package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端笔记视图对象
 */
@Data
@Schema(description = "管理端笔记")
public class AdminNoteVO {

    @Schema(description = "笔记ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "标签,逗号分隔")
    private String tags;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "图片列表")
    private List<String> images;

    @Schema(description = "类型:image/video")
    private String type;

    @Schema(description = "视频地址")
    private String videoUrl;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "作者用户ID")
    private Long userId;

    @Schema(description = "作者用户名")
    private String username;

    @Schema(description = "作者昵称")
    private String nickname;

    @Schema(description = "作者头像")
    private String userAvatar;

    @Schema(description = "状态:0待审核,1已通过,2已屏蔽,3已拒绝")
    private Integer status;

    @Schema(description = "是否置顶")
    private Boolean isSticky;

    @Schema(description = "是否推荐")
    private Boolean isRecommended;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "收藏数")
    private Integer collectCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核人用户ID")
    private Long auditUserId;

    @Schema(description = "审核人名称")
    private String auditUserName;

    @Schema(description = "拒绝原因")
    private String rejectReason;
}
