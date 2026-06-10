package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 看板待办事项
 */
@Data
@Schema(description = "待办事项")
public class DashboardTodoVO {

    @Schema(description = "待办ID")
    private Long id;

    @Schema(description = "待办类型: order/note/product")
    private String type;

    @Schema(description = "待办标题")
    private String title;

    @Schema(description = "待办描述")
    private String description;

    @Schema(description = "跳转链接")
    private String link;

    @Schema(description = "优先级: high/medium/low")
    private String priority;
}
