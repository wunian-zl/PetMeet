package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cms_comment")
@Schema(description = "笔记评论")
public class CmsComment implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_DELETED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long parentId;

    private Long replyToId;

    private Long userId;

    private String content;

    private Integer likeCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime deleteTime;
}
