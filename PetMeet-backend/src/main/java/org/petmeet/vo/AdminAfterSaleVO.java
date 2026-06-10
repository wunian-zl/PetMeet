package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Admin after-sale view")
public class AdminAfterSaleVO {
    private Long id;
    private Long orderId;
    private String orderSn;
    private Long orderItemId;

    private Long userId;
    private String username;
    private String nickname;
    private String userAvatar;

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
