package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "找回密码请求参数")
public class ResetPasswordDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "petlover")
    private String username;

    @NotBlank(message = "请输入已绑定的手机号或邮箱")
    @Schema(description = "已绑定手机号或邮箱", example = "13800138000")
    private String contact;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 18, message = "新密码长度为8-18个字符")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "新密码必须同时包含字母和数字")
    @Schema(description = "新密码", example = "PetMeet2026")
    private String newPassword;
}
