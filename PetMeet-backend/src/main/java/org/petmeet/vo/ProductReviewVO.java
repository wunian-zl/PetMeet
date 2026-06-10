package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "商品评价")
public class ProductReviewVO {
    private Long orderId;
    private String orderSn;
    private Integer score;
    private String content;
    private LocalDateTime reviewTime;
    private Long userId;
    private String username;
    private String nickname;
    private String userAvatar;
    private Integer quantity;
}
