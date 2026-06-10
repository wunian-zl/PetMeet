package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.entity.SysNotification;
import org.petmeet.service.SysNotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "Notification", description = "User notifications")
public class NotificationController {

    private final SysNotificationService notificationService;

    /**
     * 通知列表
     */
    @GetMapping("/list")
    @Operation(summary = "My notifications")
    public Result<Page<SysNotification>> list(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "1=unread only") @RequestParam(required = false) Integer unreadOnly) {
        // 查询当前用户的通知列表
        return Result.success(notificationService.pageMy(pageNum, pageSize, unreadOnly));
    }

    /**
     * 未读数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Unread count")
    public Result<Integer> unreadCount() {
        // 查询当前用户未读通知数量
        return Result.success(notificationService.unreadCount());
    }

    /**
     * 标记已读
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "Mark as read")
    public Result<Void> markRead(@PathVariable Long id) {
        // 把指定通知标记为已读
        notificationService.markRead(id);
        return Result.success("OK", null);
    }

    /**
     * 全部已读
     */
    @PutMapping("/read-all")
    @Operation(summary = "Mark all as read")
    public Result<Void> markAllRead() {
        // 把当前用户全部通知标记为已读
        notificationService.markAllRead();
        return Result.success("OK", null);
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification")
    public Result<Void> delete(@PathVariable Long id) {
        // 删除指定通知
        notificationService.deleteMy(id);
        return Result.success("OK", null);
    }

    /**
     * 批量删除通知
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "Batch delete notifications")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        // 批量删除通知
        notificationService.deleteMyBatch(ids);
        return Result.success("OK", null);
    }
}
