package org.petmeet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "广告位返回对象")
public class BannerVO {
    private Long id;
    private String title;
    private String position;
    private String slot;
    private String imageUrl;
    private String keyword;
    private String linkUrl;
    private String linkType;
}
