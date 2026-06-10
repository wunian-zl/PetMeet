package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 收货地址请求对象
 *
 * @author zjx
 */
@Data
@Schema(description = "收货地址请求参数")
public class AddressDTO {

    @Schema(description = "地址ID（修改时必传）")
    private Long id;

    @NotBlank(message = "收货人姓名不能为空")
    @Schema(description = "收货人姓名", example = "张三")
    private String name;

    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "收货人电话", example = "13800138000")
    private String phone;

    @NotBlank(message = "省份不能为空")
    @Schema(description = "省份", example = "广东省")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Schema(description = "城市", example = "深圳市")
    private String city;

    @NotBlank(message = "区县不能为空")
    @Schema(description = "区/县", example = "南山区")
    private String region;

    @NotBlank(message = "详细地址不能为空")
    @Schema(description = "详细地址", example = "科技园路XX号")
    private String detailAddress;

    @NotNull(message = "请指定是否为默认地址")
    @Schema(description = "是否默认地址：0-否，1-是", example = "1")
    private Integer isDefault;
}
