package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户视图对象
 *
 * @author zjx
 */
@Data
@Schema(description = "管理端用户信息")
public class AdminUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "电子邮箱")
    private String email;

    @Schema(description = "状态：0禁用，1正常")
    private Integer status;

    @Schema(description = "角色: user/admin/influencer")
    private String role;

    @Schema(description = "用户标签")
    private String tags;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最近活跃时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "封禁原因")
    private String banReason;

    @Schema(description = "封禁时间")
    private LocalDateTime banTime;

    // 统计信息
    @Schema(description = "发布笔记数")
    private Integer noteCount;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "消费总额")
    private java.math.BigDecimal totalSpent;

    @Schema(description = "作品获赞总数")
    private Integer totalLikeCount;

    @Schema(description = "收藏笔记数")
    private Integer collectNoteCount;
}
