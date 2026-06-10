package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.service.AdminDashboardService;
import org.petmeet.vo.DashboardStatsVO;
import org.petmeet.vo.DashboardTrendVO;
import org.petmeet.vo.DashboardTopProductVO;
import org.petmeet.vo.DashboardTodoVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端看板控制器
 *
 * @author zjx
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "管理端看板接口", description = "统计数据、趋势图、热门商品、待办事项")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 统计卡片数据
     */
    @GetMapping("/stats")
    @Operation(summary = "统计卡片数据", description = "获取销售额、订单量、新用户、待审核等统计")
    public Result<DashboardStatsVO> stats(
            @Parameter(description = "时间范围: today/week/month") @RequestParam(defaultValue = "today") String range) {
        // 查询看板顶部统计卡片数据
        return Result.success(adminDashboardService.getStats(range));
    }

    /**
     * 趋势图数据
     */
    @GetMapping("/trend")
    @Operation(summary = "趋势图数据", description = "获取流量/订单趋势")
    public Result<DashboardTrendVO> trend(
            @Parameter(description = "时间范围: week/month") @RequestParam(defaultValue = "week") String range) {
        // 查询看板趋势图数据
        return Result.success(adminDashboardService.getTrend(range));
    }

    /**
     * 分类销售占比
     */
    @GetMapping("/category-sales")
    @Operation(summary = "分类销售占比")
    public Result<List<Map<String, Object>>> categorySales() {
        // 查询不同分类的销售占比
        return Result.success(adminDashboardService.getCategorySales());
    }

    /**
     * 热门商品
     */
    @GetMapping("/top-products")
    @Operation(summary = "热门商品TOP5")
    public Result<List<DashboardTopProductVO>> topProducts() {
        // 查询销量靠前的商品
        return Result.success(adminDashboardService.getTopProducts());
    }

    /**
     * 待办事项
     */
    @GetMapping("/todos")
    @Operation(summary = "待办事项")
    public Result<List<DashboardTodoVO>> todos() {
        // 查询管理员需要优先处理的事项
        return Result.success(adminDashboardService.getTodos());
    }
}
