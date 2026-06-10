package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.entity.SysUser;
import org.petmeet.service.AdminUserService;
import org.petmeet.vo.AdminUserVO;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端用户管理控制器
 *
 * @author zjx
 */
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "Admin User", description = "User management CRUD, ban/unban")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "User list", description = "Paged users with keyword, status, role filters")
    public Result<Page<AdminUserVO>> list(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Role filter") @RequestParam(required = false) String role,
            @Parameter(description = "Status filter") @RequestParam(required = false) Integer status) {
        // 查询管理端用户列表
        return Result.success(adminUserService.pageList(pageNum, pageSize, keyword, role, status));
    }

    /**
     * 用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "User detail")
    public Result<AdminUserVO> detail(@PathVariable Long id) {
        // 查询指定用户详情
        return Result.success(adminUserService.getDetail(id));
    }

    /**
     * 新增用户
     */
    @PostMapping
    @Operation(summary = "Create user")
    public Result<Long> create(@RequestBody SysUser user) {
        // 调用业务层创建用户
        Long id = adminUserService.createUser(user);
        return Result.success("Created", id);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        // 先补齐用户 id，再调用业务层更新
        user.setId(id);
        adminUserService.updateUser(user);
        return Result.success("Updated", null);
    }

    /**
     * 封禁用户
     */
    @PutMapping("/{id}/ban")
    @Operation(summary = "Ban user")
    public Result<Void> ban(
            @PathVariable Long id,
            @Parameter(description = "Ban reason") @RequestParam(required = false) String reason) {
        // 调用业务层封禁用户
        adminUserService.banUser(id, reason);
        return Result.success("Banned", null);
    }

    /**
     * 解封用户
     */
    @PutMapping("/{id}/unban")
    @Operation(summary = "Unban user")
    public Result<Void> unban(@PathVariable Long id) {
        // 调用业务层解封用户
        adminUserService.unbanUser(id);
        return Result.success("Unbanned", null);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    public Result<Void> delete(@PathVariable Long id) {
        // 调用业务层删除用户
        adminUserService.deleteUser(id);
        return Result.success("Deleted", null);
    }

    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset password")
    public Result<String> resetPassword(@PathVariable Long id) {
        // 调用业务层重置密码
        String newPassword = adminUserService.resetPassword(id);
        return Result.success("Password reset", newPassword);
    }

    /**
     * 强制下线
     */
    @PostMapping("/{id}/force-logout")
    @Operation(summary = "Force logout")
    public Result<Void> forceLogout(@PathVariable Long id) {
        // 调用业务层强制用户下线
        adminUserService.forceLogout(id);
        return Result.success("Forced logout", null);
    }

    /**
     * 统一头像风格
     */
    @PostMapping("/{id}/harmonize-avatar")
    @Operation(summary = "Harmonize avatar")
    public Result<Void> harmonizeAvatar(@PathVariable Long id) {
        // 调用业务层统一头像风格
        adminUserService.harmonizeAvatar(id);
        return Result.success("Harmonized", null);
    }
}
