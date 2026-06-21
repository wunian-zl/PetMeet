package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@Schema(description = "用户实体")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_INFLUENCER = "influencer";

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "电子邮箱")
    private String email;

    @Schema(description = "性别：male/female/other")
    private String gender;

    @Schema(description = "出生日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Schema(description = "状态：0禁用，1正常")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // 用户管理相关的扩展字段
    @Schema(description = "角色: user/admin/influencer")
    private String role;

    @Schema(description = "用户标签: CSV格式")
    private String tags;

    @Schema(description = "最近活跃时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "封禁原因")
    private String banReason;

    @Schema(description = "封禁时间")
    private LocalDateTime banTime;
}
