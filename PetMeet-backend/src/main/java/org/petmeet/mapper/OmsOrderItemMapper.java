package org.petmeet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.petmeet.dto.TopProductSalesDTO;
import org.petmeet.entity.OmsOrderItem;

import java.util.List;

@Mapper
public interface OmsOrderItemMapper extends BaseMapper<OmsOrderItem> {

    @Select("""
            SELECT
              oi.product_id AS productId,
              SUM(oi.quantity) AS sales
            FROM oms_order_item oi
            INNER JOIN oms_order o ON o.id = oi.order_id
            INNER JOIN pms_product p ON p.id = oi.product_id
            WHERE o.status IN (1, 2, 3)
              AND p.status = 1
              AND p.is_deleted = 0
            GROUP BY oi.product_id
            ORDER BY sales DESC
            LIMIT #{limit}
            """)
    List<TopProductSalesDTO> selectTopPaidProductSales(@Param("limit") int limit);
}
