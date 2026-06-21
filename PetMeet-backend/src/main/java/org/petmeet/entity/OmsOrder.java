package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oms_order")
public class OmsOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderSn;
    @TableField(exist = false)
    private String orderNo; // 别名, 用于前端兼容
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private Integer status;
    private Integer payType;
    private String paySn;
    private String tradeNo;
    private Integer reviewStatus;
    private Integer reviewScore;
    private String reviewContent;
    private LocalDateTime reviewTime;
    private String receiverInfo;
    private LocalDateTime payTime;

    // 发货相关
    private String shipCompany;
    private String trackingNo;
    private LocalDateTime shipTime;

    // 收货人信息 (从receiverInfo解析或单独存储)
    private String receiver;
    private String phone;
    private String address;
    private String remark;
    private Integer userDeleted;
    private Integer adminDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static final int STATUS_PENDING_PAY = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_SHIPPED = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_CLOSED = 4;
    public static final int STATUS_REFUNDING = 5;

    public static final int REVIEW_PENDING = 0;
    public static final int REVIEW_DONE = 1;

    public static final int DELETE_VISIBLE = 0;
    public static final int DELETE_DELETED = 1;
}
