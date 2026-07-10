package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oms_after_sale_log")
public class OmsAfterSaleLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long afterSaleId;
    private Integer fromStatus;
    private Integer toStatus;
    private String action;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
