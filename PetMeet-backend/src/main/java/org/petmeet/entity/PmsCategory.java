package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("pms_category")
@Schema(description = "商品分类实体")
public class PmsCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图标URL")
    private String icon;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "状态：0禁用，1启用")
    private Integer status;
}
