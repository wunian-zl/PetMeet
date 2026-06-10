package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 看板趋势图数据
 */
@Data
@Schema(description = "Dashboard趋势")
public class DashboardTrendVO {

    @Schema(description = "日期标签")
    private List<String> labels;

    @Schema(description = "访问量数据")
    private List<Integer> pageViews;

    @Schema(description = "订单量数据")
    private List<Integer> orderCounts;

    @Schema(description = "销售额数据")
    private List<Double> salesData;
}
