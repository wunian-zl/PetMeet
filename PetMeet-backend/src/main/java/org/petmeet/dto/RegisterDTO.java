package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求对象
 *
 * @author zjx
 */
@Data
@Schema(description = "注册请求参数")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度为2-20个字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$", message = "用户名只能包含汉字、字母、数字和下划线")
    @Schema(description = "用户名", example = "petlover")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度为8-64个字符")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
    @Schema(description = "密码", example = "PetMeet2026")
    private String password;

    @Size(max = 50, message = "昵称最多50个字符")
    @Schema(description = "昵称", example = "宠物爱好者")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}
