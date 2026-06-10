package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "订单商品项")
public class OrderItemVO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImg;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
