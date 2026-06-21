package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "评论视图")
public class CommentVO {
    private Long id;
    private Long noteId;
    private Long parentId;
    private Long replyToId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;
    private String userNickname;
    private String userAvatar;
    private Boolean mine;
    private Boolean author;
    private Boolean liked;
    private Integer likeCount;
    private Integer replyCount;
    private List<CommentVO> replies;
    private String replyToNickname;
    private String replyToContent;
    private Boolean deleted;
}
