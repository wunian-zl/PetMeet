package org.petmeet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AfterSaleLogVO {
    private Long id;
    private Long afterSaleId;
    private Integer fromStatus;
    private String fromStatusDesc;
    private Integer toStatus;
    private String toStatusDesc;
    private String action;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
