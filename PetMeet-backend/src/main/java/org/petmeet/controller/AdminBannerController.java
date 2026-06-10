package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.entity.CmsBanner;
import org.petmeet.service.CmsBannerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/banner")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "管理端广告位接口", description = "广告位管理CRUD")
public class AdminBannerController {

    private final CmsBannerService cmsBannerService;

    /**
     * 广告位列表
     */
    @GetMapping("/list")
    @Operation(summary = "广告位列表", description = "分页查询广告位")
    public Result<Page<CmsBanner>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "位置编码") @RequestParam(required = false) String position,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        // 按条件组装查询条件
        LambdaQueryWrapper<CmsBanner> wrapper = new LambdaQueryWrapper<>();
        if (position != null && !position.isEmpty()) {
            wrapper.eq(CmsBanner::getPosition, position);
        }
        if (status != null) {
            wrapper.eq(CmsBanner::getStatus, status);
        }
        wrapper.orderByAsc(CmsBanner::getSort, CmsBanner::getId);

        // 调用业务层查询广告位分页数据
        Page<CmsBanner> page = cmsBannerService.page(new Page<>(pageNum, pageSize), wrapper);
        // 若“近N天”筛选已过期，则自动归一化为“不限制”，避免管理端一直看到旧配置
        cmsBannerService.normalizeExpiredRecentDays(page.getRecords());
        return Result.success(page);
    }

    /**
     * 新增广告位
     */
    @PostMapping
    @Operation(summary = "新增广告位")
    public Result<Long> create(@RequestBody CmsBanner banner) {
        // 保存广告位数据
        cmsBannerService.save(banner);
        return Result.success("创建成功", banner.getId());
    }

    /**
     * 更新广告位
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新广告位")
    public Result<Void> update(@PathVariable Long id, @RequestBody CmsBanner banner) {
        // 先补齐主键，再更新广告位
        banner.setId(id);
        cmsBannerService.updateById(banner);
        return Result.success("更新成功", null);
    }

    /**
     * 修改广告位状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用广告位")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @Parameter(description = "状态: 0禁用/1启用") @RequestParam Integer status) {
        // 只更新广告位状态字段
        CmsBanner banner = new CmsBanner();
        banner.setId(id);
        banner.setStatus(status);
        cmsBannerService.updateById(banner);
        return Result.success(status == 1 ? "已启用" : "已禁用", null);
    }

    /**
     * 删除广告位
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除广告位")
    public Result<Void> delete(@PathVariable Long id) {
        // 根据 id 删除广告位
        cmsBannerService.removeById(id);
        return Result.success("删除成功", null);
    }
}
