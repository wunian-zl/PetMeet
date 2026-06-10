package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端商品视图对象
 */
@Data
@Schema(description = "管理端商品信息")
public class AdminProductVO {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题")
    private String subTitle;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "预警库存")
    private Integer warningStock;

    @Schema(description = "排序权重")
    private Integer sortWeight;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "浏览量")
    private Integer views;

    @Schema(description = "适用宠物")
    private String petType;

    @Schema(description = "关联笔记数")
    private Integer relatedNoteCount;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "封面图(兼容字段)")
    private String coverImg;

    @Schema(description = "主图组(JSON)")
    private String coverImgs;

    @Schema(description = "详情图(逗号分隔)")
    private String images;

    @Schema(description = "详情图(JSON)")
    private String detailImgs;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "状态: 0下架/1上架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
