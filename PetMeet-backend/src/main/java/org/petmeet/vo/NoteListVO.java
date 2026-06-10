package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "笔记列表项")
public class NoteListVO {
    private Long id;
    private Long userId;
    private String title;
    private String category;
    private List<String> tags;
    private String coverImg;
    private String coverThumb;
    private String type;
    private String videoUrl;
    private Integer likeCount;
    private Integer collectCount;
    private Integer status;
    private String statusDesc;
    private String authorNickname;
    private String authorAvatar;
    private Boolean liked;
    private Integer productCount;
    private LocalDateTime createTime;
}
