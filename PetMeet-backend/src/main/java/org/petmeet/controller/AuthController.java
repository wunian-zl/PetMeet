package org.petmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.LoginDTO;
import org.petmeet.dto.RegisterDTO;
import org.petmeet.service.SysUserService;
import org.petmeet.vo.LoginVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "用户注册、登录、登出")
public class AuthController {

    private final SysUserService sysUserService;

    /**
     * 注册功能
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        // 调用业务层完成注册
        LoginVO vo = sysUserService.register(dto);
        return Result.success("注册成功", vo);
    }

    /**
     * 登录功能
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        // 调用业务层完成登录校验
        LoginVO vo = sysUserService.login(dto);
        return Result.success("登录成功", vo);
    }

    /**
     * 登出功能
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        // 清除当前用户登录态
        sysUserService.logout();
        return Result.success("登出成功", null);
    }
}
