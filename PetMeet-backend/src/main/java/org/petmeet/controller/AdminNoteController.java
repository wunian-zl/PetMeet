package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.service.AdminNoteService;
import org.petmeet.vo.AdminNoteVO;
import org.petmeet.vo.AdminNoteStatsVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端笔记控制器
 */
@RestController
@RequestMapping("/admin/note")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "Admin Note", description = "Note audit and management")
public class AdminNoteController {

    private final AdminNoteService adminNoteService;

    /**
     * 笔记列表
     */
    @GetMapping("/list")
    @Operation(summary = "List notes")
    public Result<Page<AdminNoteVO>> list(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Status") @RequestParam(required = false) Integer status,
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Category") @RequestParam(required = false) String category,
            @Parameter(description = "Tags (comma separated)") @RequestParam(required = false) String tag) {
        // 查询管理端笔记列表
        return Result.success(adminNoteService.pageList(pageNum, pageSize, status, keyword, category, tag));
    }

    /**
     * 笔记统计
     */
    @GetMapping("/stats")
    @Operation(summary = "Stats")
    public Result<AdminNoteStatsVO> stats() {
        // 查询笔记统计数据
        return Result.success(adminNoteService.getStats());
    }

    /**
     * 笔记详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detail")
    public Result<AdminNoteVO> detail(@PathVariable Long id) {
        // 查询指定笔记详情
        return Result.success(adminNoteService.getDetail(id));
    }

    /**
     * 审核通过笔记
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve")
    public Result<Void> approve(@PathVariable Long id) {
        // 调用业务层通过审核
        adminNoteService.approve(id);
        return Result.success("Approved", null);
    }

    /**
     * 驳回笔记
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject")
    public Result<Void> reject(
            @PathVariable Long id,
            @Parameter(description = "Reject reason") @RequestParam(required = false) String reason) {
        // 调用业务层驳回笔记
        adminNoteService.reject(id, reason);
        return Result.success("Rejected", null);
    }

    /**
     * 置顶笔记
     */
    @PutMapping("/{id}/sticky")
    @Operation(summary = "Toggle sticky")
    public Result<Boolean> sticky(@PathVariable Long id) {
        // 切换笔记置顶状态
        boolean isSticky = adminNoteService.toggleSticky(id);
        return Result.success(isSticky ? "Sticky" : "Unsticky", isSticky);
    }

    /**
     * 推荐笔记
     */
    @PutMapping("/{id}/recommend")
    @Operation(summary = "Toggle recommend")
    public Result<Boolean> recommend(@PathVariable Long id) {
        // 切换笔记推荐状态
        boolean isRecommend = adminNoteService.toggleRecommend(id);
        return Result.success(isRecommend ? "Recommended" : "Unrecommended", isRecommend);
    }

    /**
     * 屏蔽笔记
     */
    @PutMapping("/{id}/shield")
    @Operation(summary = "Toggle shield")
    public Result<Boolean> shield(
            @PathVariable Long id,
            @Parameter(description = "Down reason (optional)") @RequestParam(required = false) String reason) {
        // 切换笔记屏蔽状态
        boolean isShielded = adminNoteService.toggleShield(id, reason);
        return Result.success(isShielded ? "Shielded" : "Unshielded", isShielded);
    }

    // 兼容新旧版管理端，两个路径都保留。
    /**
     * 软删除笔记
     */
    @PutMapping({"/{id}/soft-delete", "/{id}/softDelete"})
    @Operation(summary = "Soft delete")
    public Result<Void> softDelete(
            @PathVariable Long id,
            @Parameter(description = "Delete reason (optional)") @RequestParam(required = false) String reason) {
        // 调用业务层软删除笔记
        adminNoteService.softDelete(id, reason);
        return Result.success("Soft deleted", null);
    }

    /**
     * 批量操作笔记
     */
    @PostMapping("/batch")
    @Operation(summary = "Batch action")
    public Result<Void> batch(
            @Parameter(description = "Action: approve/reject/shield/softDelete/delete") @RequestParam String action,
            @RequestBody List<Long> ids) {
        // 调用业务层执行批量操作
        adminNoteService.batchAction(action, ids);
        return Result.success("Batch done", null);
    }
}
