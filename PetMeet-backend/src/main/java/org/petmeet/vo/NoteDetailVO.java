package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "笔记详情")
public class NoteDetailVO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String category;
    private List<String> tags;
    private String coverImg;
    private List<String> images;
    private String type;
    private String videoUrl;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private Integer status;
    private String statusDesc;
    private String authorNickname;
    private String authorAvatar;
    private Boolean liked;
    private Boolean collected;
    private List<ProductListVO> products;
    private LocalDateTime createTime;
}
