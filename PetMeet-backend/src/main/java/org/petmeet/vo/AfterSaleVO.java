package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "After-sale info")
public class AfterSaleVO {
    private Long id;
    private Long orderId;
    private String orderSn;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productImg;
    private BigDecimal price;
    private Integer quantity;
    private Integer type;
    private String typeDesc;
    private Integer status;
    private String statusDesc;
    private String reason;
    private String description;
    private List<String> evidenceImages;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
