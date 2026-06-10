package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.service.FollowService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
@Tag(name = "关注接口", description = "关注/取消关注与统计")
public class FollowController {

    private final FollowService followService;

    /**
     * 关注或取消关注
     */
    @PostMapping("/{userId}")
    @SaCheckLogin
    @Operation(summary = "关注/取消关注")
    public Result<Boolean> toggleFollow(@PathVariable Long userId) {
        // 切换当前用户的关注状态
        boolean followed = followService.toggleFollow(userId);
        return Result.success(followed ? "关注成功" : "已取消关注", followed);
    }

    /**
     * 是否已关注
     */
    @GetMapping("/status/{userId}")
    @SaCheckLogin
    @Operation(summary = "是否已关注")
    public Result<Boolean> isFollowing(@PathVariable Long userId) {
        // 查询当前用户是否已关注目标用户
        return Result.success(followService.isFollowing(userId));
    }

    /**
     * 关注统计
     */
    @GetMapping("/count/{userId}")
    @Operation(summary = "关注/粉丝数")
    public Result<Map<String, Integer>> count(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        // 统计粉丝数和关注数
        Map<String, Integer> data = new HashMap<>();
        data.put("followers", followService.countFollowers(userId));
        data.put("following", followService.countFollowing(userId));
        return Result.success(data);
    }

    /**
     * 粉丝列表
     */
    @GetMapping("/followers/{userId}")
    @Operation(summary = "粉丝列表")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<org.petmeet.vo.FollowUserVO>> followers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 查询指定用户的粉丝列表
        return Result.success(followService.pageFollowers(userId, pageNum, pageSize));
    }

    /**
     * 关注列表
     */
    @GetMapping("/following/{userId}")
    @Operation(summary = "关注列表")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<org.petmeet.vo.FollowUserVO>> following(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 查询指定用户的关注列表
        return Result.success(followService.pageFollowing(userId, pageNum, pageSize));
    }
}
