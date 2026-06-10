package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pms_product")
@Schema(description = "商品实体")
public class PmsProduct implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
    private String name;
    private String subTitle;
    private BigDecimal price;
    private Integer stock;
    private String unit; // 单位 (如: kg, 包, 个)

    @Version
    private Integer version;

    private String coverImg;
    private String coverImgs;
    private String detailImgs;
    private String description;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 新增字段
    private Integer warningStock; // 预警库存
    private Integer sortWeight; // 排序权重
    private Integer sales; // 销量 (持久化)
    @TableLogic
    private Integer isDeleted; // 逻辑删除 (0:正常, 1:删除)
    private String petType; // 适用宠物 (cat/dog/general)

    // 动态权重因子
    private Integer views; // 浏览量
    private Integer relatedNoteCount; // 关联笔记数
}
