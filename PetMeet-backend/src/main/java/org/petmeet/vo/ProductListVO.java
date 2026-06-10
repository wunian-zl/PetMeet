package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "商品列表项")
public class ProductListVO {
    private Long id;
    private Long categoryId;
    private String name;
    private String subTitle;
    private BigDecimal price;
    private Integer stock;
    private String coverImg;
}
