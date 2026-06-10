package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_follow")
@Schema(description = "关注关系")
public class SysFollow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "关注者ID")
    private Long followerId;

    @Schema(description = "被关注者ID")
    private Long followeeId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
