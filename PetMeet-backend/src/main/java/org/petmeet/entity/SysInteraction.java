package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_interaction")
public class SysInteraction implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long targetId;
    private Integer type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static final int TYPE_LIKE_NOTE = 1;
    public static final int TYPE_COLLECT_NOTE = 2;
}
