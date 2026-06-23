package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.AddressDTO;
import org.petmeet.dto.ChangePasswordDTO;
import org.petmeet.entity.SysUser;
import org.petmeet.entity.UmsAddress;
import org.petmeet.service.SysUserService;
import org.petmeet.service.UmsAddressService;
import org.petmeet.vo.UserInfoVO;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "用户接口", description = "用户信息、收货地址管理")
public class UserController {

    private final SysUserService sysUserService;
    private final UmsAddressService umsAddressService;

    /**
     * 当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfoVO> getCurrentUser() {
        // 查询当前登录用户信息
        return Result.success(sysUserService.getCurrentUser());
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateUserInfo(@RequestBody SysUser user) {
        // 调用业务层更新用户资料
        sysUserService.updateUserInfo(user);
        return Result.success("更新成功", null);
    }

    /**
     * 修改当前用户密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改当前用户密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        sysUserService.changePassword(dto);
        return Result.success("密码修改成功", null);
    }

    /**
     * 新增收货地址
     */
    @PostMapping("/address")
    @Operation(summary = "新增收货地址")
    public Result<Long> addAddress(@Valid @RequestBody AddressDTO dto) {
        // 调用业务层保存收货地址
        Long id = umsAddressService.saveAddress(dto);
        return Result.success("添加成功", id);
    }

    /**
     * 更新收货地址
     */
    @PutMapping("/address")
    @Operation(summary = "更新收货地址")
    public Result<Void> updateAddress(@Valid @RequestBody AddressDTO dto) {
        // 调用业务层更新收货地址
        umsAddressService.updateAddress(dto);
        return Result.success("更新成功", null);
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/address/{id}")
    @Operation(summary = "删除收货地址")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        // 调用业务层删除收货地址
        umsAddressService.deleteAddress(id);
        return Result.success("删除成功", null);
    }

    /**
     * 收货地址列表
     */
    @GetMapping("/address/list")
    @Operation(summary = "获取收货地址列表")
    public Result<List<UmsAddress>> listAddress() {
        // 查询当前用户的收货地址列表
        return Result.success(umsAddressService.listByCurrentUser());
    }
}
