package org.petmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.LoginDTO;
import org.petmeet.service.SysUserService;
import org.petmeet.vo.LoginVO;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端认证控制器
 * 只允许角色为 admin 的用户登录
 *
 * @author zjx
 */
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Tag(name = "管理端认证接口", description = "管理员登录、登出")
public class AdminAuthController {

    private final SysUserService sysUserService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        // 调用业务层完成管理员登录校验
        LoginVO vo = sysUserService.adminLogin(dto);
        return Result.success("登录成功", vo);
    }

    /**
     * 管理员登出
     */
    @PostMapping("/logout")
    @Operation(summary = "管理员登出")
    public Result<Void> logout() {
        // 清除当前管理员登录态
        sysUserService.logout();
        return Result.success("登出成功", null);
    }
}
