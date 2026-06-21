package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cms_note")
@Schema(description = "笔记实体")
public class CmsNote implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_SHIELDED = 2;
    public static final int STATUS_REJECTED = 3;
    public static final int STATUS_USER_OFF_SHELF = 4;
    public static final int STATUS_USER_DELETED = 5;
    public static final int STATUS_ADMIN_SOFT_DELETED = 6;
    public static final int DELETE_VISIBLE = 0;
    public static final int DELETE_DELETED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String content;
    @Schema(description = "社区分类")
    private String category;
    @Schema(description = "标签(逗号分隔)")
    private String tags;
    private String coverImg;
    private String images;

    @Schema(description = "笔记类型: image(图文)/video(视频)")
    private String type;

    @Schema(description = "视频地址(type=video时)")
    private String videoUrl;
    private Integer likeCount;
    private Integer collectCount;
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @Schema(description = "是否推荐")
    private Boolean isRecommended;

    @Schema(description = "是否置顶")
    private Boolean isSticky;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 审核字段，供管理端内容审核使用。
    private LocalDateTime auditTime;
    private Long auditUserId;
    private String rejectReason;
}
