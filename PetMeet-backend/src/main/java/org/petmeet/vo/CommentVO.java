package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "评论信息")
public class CommentVO {
    private Long id;
    private Long noteId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;
    private String userNickname;
    private String userAvatar;
    private Boolean mine;
}
