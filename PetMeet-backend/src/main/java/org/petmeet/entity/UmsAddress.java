package org.petmeet.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("ums_address")
@Schema(description = "收货地址实体")
public class UmsAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "收货人姓名")
    private String name;

    @Schema(description = "收货人电话")
    private String phone;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String region;

    @Schema(description = "详细地址")
    private String detailAddress;

    @Schema(description = "是否默认：0否，1是")
    private Integer isDefault;
}
