package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("oms_after_sale")
public class OmsAfterSale implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Integer type;
    private String reason;
    private String description;
    /**
     * 凭证图片列表，按 JSON 数组字符串保存，例如 ["/images/a.png","/images/b.png"]。
     */
    private String evidenceImages;
    private Integer status;
    private Integer userDeleted;
    private Integer adminDeleted;
    private String handleRemark;
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_REJECTED = 3;
    public static final int STATUS_CANCELED = 4;
}
