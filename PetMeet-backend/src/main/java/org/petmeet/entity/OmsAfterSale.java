package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private BigDecimal refundAmount;
    private String returnAddress;
    private String returnCompany;
    private String returnTrackingNo;
    private LocalDateTime returnShipTime;
    private LocalDateTime returnReceiveTime;
    private String exchangeCompany;
    private String exchangeTrackingNo;
    private LocalDateTime exchangeShipTime;
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
    public static final int STATUS_WAIT_BUYER_RETURN = 5;
    public static final int STATUS_WAIT_MERCHANT_RECEIVE = 6;
    public static final int STATUS_REFUNDING = 7;
    public static final int STATUS_EXCHANGE_SHIPPED = 8;

    public static final int DELETE_VISIBLE = 0;
    public static final int DELETE_DELETED = 1;

    public static List<Integer> activeStatuses() {
        return List.of(
                STATUS_PENDING,
                STATUS_PROCESSING,
                STATUS_WAIT_BUYER_RETURN,
                STATUS_WAIT_MERCHANT_RECEIVE,
                STATUS_REFUNDING,
                STATUS_EXCHANGE_SHIPPED
        );
    }

    public static boolean isTerminalStatus(Integer status) {
        return status != null && (status == STATUS_COMPLETED
                || status == STATUS_REJECTED
                || status == STATUS_CANCELED);
    }
}
