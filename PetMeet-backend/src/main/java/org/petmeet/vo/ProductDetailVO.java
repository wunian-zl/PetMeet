package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "商品详情")
public class ProductDetailVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String subTitle;
    private BigDecimal price;
    private Integer stock;
    private String coverImg;
    private List<String> coverImgs;
    private List<String> detailImgs;
    private String description;
    private Integer reviewCount;
    private Double reviewAvgScore;
    private List<ProductReviewVO> reviewList;
}
