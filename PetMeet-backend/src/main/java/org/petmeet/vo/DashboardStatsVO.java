package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 看板统计卡片数据
 */
@Data
@Schema(description = "Dashboard统计")
public class DashboardStatsVO {

    @Schema(description = "销售额")
    private BigDecimal totalSales;

    @Schema(description = "销售额环比")
    private Double salesChange;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "订单环比")
    private Double orderChange;

    @Schema(description = "新用户数")
    private Integer newUserCount;

    @Schema(description = "新用户环比")
    private Double userChange;

    @Schema(description = "待审核内容数")
    private Integer pendingNoteCount;

    @Schema(description = "待发货订单数")
    private Integer pendingShipCount;
}
