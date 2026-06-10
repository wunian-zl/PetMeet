package org.petmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.entity.CmsBanner;
import org.petmeet.service.CmsBannerService;
import org.petmeet.vo.BannerVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/banner")
@RequiredArgsConstructor
@Tag(name = "广告位接口", description = "商城广告位展示")
public class BannerController {

    private final CmsBannerService cmsBannerService;

    /**
     * 根据位置获取广告位
     */
    @GetMapping("/position/{position}")
    @Operation(summary = "按位置获取广告位")
    public Result<List<BannerVO>> listByPosition(
            @Parameter(description = "位置编码") @PathVariable String position) {
        // 查询指定位置下启用中的广告位
        List<CmsBanner> banners = cmsBannerService.listActiveByPosition(position);

        // 把实体对象转换成前端展示用的 VO
        List<BannerVO> vos = banners.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(vos);
    }

    /**
     * 转换广告位数据
     */
    private BannerVO toVO(CmsBanner banner) {
        BannerVO vo = new BannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setPosition(banner.getPosition());
        vo.setSlot(banner.getSlot());
        vo.setImageUrl(banner.getImageUrl());
        vo.setKeyword(banner.getKeyword());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setLinkType(banner.getLinkType());
        return vo;
    }
}
