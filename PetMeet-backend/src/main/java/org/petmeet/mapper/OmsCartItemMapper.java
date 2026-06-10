package org.petmeet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.petmeet.entity.OmsCartItem;
import java.time.LocalDateTime;

@Mapper
public interface OmsCartItemMapper extends BaseMapper<OmsCartItem> {
    @Select("SELECT * FROM oms_cart_item WHERE user_id = #{userId} AND product_id = #{productId} LIMIT 1")
    OmsCartItem selectByUserAndProductIgnoreDeleted(@Param("userId") Long userId, @Param("productId") Long productId);

    @Update("UPDATE oms_cart_item SET quantity = #{quantity}, is_deleted = 0, selected = 1 WHERE id = #{id}")
    int updateQuantityAndRevive(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Insert("""
        INSERT INTO oms_cart_item (user_id, product_id, quantity, create_time, selected, is_deleted)
        VALUES (#{userId}, #{productId}, #{quantity}, #{createTime}, 1, 0)
        ON DUPLICATE KEY UPDATE
            quantity = IF(is_deleted = 1, VALUES(quantity), quantity + VALUES(quantity)),
            is_deleted = 0,
            selected = 1
        """)
    int upsertCartItem(@Param("userId") Long userId,
                       @Param("productId") Long productId,
                       @Param("quantity") Integer quantity,
                       @Param("createTime") LocalDateTime createTime);
}
