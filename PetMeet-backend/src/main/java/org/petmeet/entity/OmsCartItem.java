package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("oms_cart_item")
public class OmsCartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int DELETE_VISIBLE = 0;
    public static final int DELETE_DELETED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Boolean selected = true;
}
