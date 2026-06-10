package org.petmeet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求对象
 *
 * @author zjx
 */
@Data
@Schema(description = "登录请求参数")
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "petlover")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "PetMeet2026")
    private String password;
}
