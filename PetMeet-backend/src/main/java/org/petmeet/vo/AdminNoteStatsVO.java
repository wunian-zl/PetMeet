package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端笔记统计视图对象
 */
@Data
@Schema(description = "笔记统计信息")
public class AdminNoteStatsVO {

    @Schema(description = "待审核数量")
    private Integer pendingCount;

    @Schema(description = "今日新增")
    private Integer todayCount;

    @Schema(description = "今日已通过")
    private Integer todayApprovedCount;

    @Schema(description = "今日已拒绝")
    private Integer todayRejectedCount;

    @Schema(description = "已发布总数")
    private Integer publishedCount;

    @Schema(description = "已屏蔽数量")
    private Integer shieldedCount;

    @Schema(description = "已拒绝 count")
    private Integer rejectedCount;

    @Schema(description = "用户主动下架数量")
    private Integer userOffShelfCount;

    @Schema(description = "用户删除数量")
    private Integer userDeletedCount;

    @Schema(description = "管理员软删除数量")
    private Integer adminSoftDeletedCount;
}
