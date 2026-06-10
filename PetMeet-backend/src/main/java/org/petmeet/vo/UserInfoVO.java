package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户信息")
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String gender;
    private java.time.LocalDate birthDate;
    private String tags;
}
