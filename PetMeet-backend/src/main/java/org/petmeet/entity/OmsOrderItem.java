package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("oms_order_item")
public class OmsOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImg;
    private BigDecimal price;
    private Integer quantity;
}
