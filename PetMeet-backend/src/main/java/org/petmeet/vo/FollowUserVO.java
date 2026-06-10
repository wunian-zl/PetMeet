package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "关注/粉丝列表用户")
public class FollowUserVO {
    private Long id;
    private String nickname;
    private String avatar;
    @Schema(description = "当前登录用户是否已关注该用户")
    private Boolean followed;
}
