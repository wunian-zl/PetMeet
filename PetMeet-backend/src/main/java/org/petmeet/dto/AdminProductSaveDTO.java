package org.petmeet.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminProductSaveDTO {
    private Long id;
    private Long categoryId;
    private String name;
    private String subTitle;
    private BigDecimal price;
    private Integer stock;
    private String unit;
    private String coverImg;
    private String coverImgs;
    private String detailImgs;
    private String description;
    private Integer status;
    private Integer warningStock;
    private Integer sortWeight;
    private String petType;
    private Integer sales;
    private Integer views;
    private Integer relatedNoteCount;
}
