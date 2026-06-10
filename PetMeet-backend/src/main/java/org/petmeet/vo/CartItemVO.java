package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "购物车项")
public class CartItemVO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImg;
    private BigDecimal price;
    private Integer stock;
    private Integer quantity;
    private BigDecimal subtotal;
    private Integer productStatus;
    private LocalDateTime createTime;
}
