package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cms_complaint")
public class CmsComplaint implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int DELETE_VISIBLE = 0;
    public static final int DELETE_DELETED = 1;
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_HANDLED = 1;
    public static final int STATUS_REJECTED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private String targetType;

    private Long commentId;

    /**
     * 父投诉 ID，用来串起再次投诉的链路。
     */
    private Long parentId;

    private Long userId;

    private String reason;

    private String content;

    private String evidenceImages;

    private Integer status;

    /**
     * 管理员处理备注或处理结果。
     */
    private String handleRemark;

    /**
     * 0 表示可见，1 表示用户已删除。
     */
    private Integer userDeleted;
    /**
     * 0 表示可见，1 表示管理端已删除。
     */
    private Integer adminDeleted;

    /**
     * 0 表示未反馈，1 表示满意，2 表示不满意。
     */
    private Integer feedbackStatus;

    private String feedbackContent;

    private LocalDateTime feedbackTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime handleTime;

    private Long handlerId;
}
