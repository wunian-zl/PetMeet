package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cms_banner")
@Schema(description = "广告位实体")
public class CmsBanner implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String position; // 广告位位置，如 SHOP_TOP
    private String slot; // 位置槽位，如 main/sub1/sub2
    private String imageUrl;
    /**
     * 预设关键词：用于“养宠科普栏目”跳转到社区时自动搜索
     */
    private String keyword;
    private String linkUrl;
    private String linkType; // url/internal/product/category
    private Integer sort;
    private Integer status; // 0禁用 1启用

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
