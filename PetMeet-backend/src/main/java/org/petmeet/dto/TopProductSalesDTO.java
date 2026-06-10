package org.petmeet.dto;

import lombok.Data;

/**
 * 订单维度统计：商品销量汇总
 */
@Data
public class TopProductSalesDTO {
    private Long productId;
    private Long sales;
}

