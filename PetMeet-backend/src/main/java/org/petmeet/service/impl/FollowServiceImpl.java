package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.petmeet.entity.SysFollow;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.SysFollowMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.FollowService;
import org.petmeet.vo.FollowUserVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final SysFollowMapper followMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 切换关注
     */
    @Override
    public boolean toggleFollow(Long followeeId) {
        Long followerId = StpUtil.getLoginIdAsLong();
        if (followeeId == null || followerId.equals(followeeId)) {
            throw AppException.badRequest("不能关注自己");
        }

        LambdaQueryWrapper<SysFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFollow::getFollowerId, followerId)
                .eq(SysFollow::getFolloweeId, followeeId);
        SysFollow existing = followMapper.selectOne(wrapper);
        if (existing != null) {
            // 已关注时取消关注
            followMapper.deleteById(existing.getId());
            return false;
        }

        // 未关注时新增关注关系
        SysFollow follow = new SysFollow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        follow.setCreateTime(LocalDateTime.now());
        followMapper.insert(follow);
        return true;
    }

    /**
     * 是否已关注
     */
    @Override
    public boolean isFollowing(Long followeeId) {
        Long followerId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<SysFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFollow::getFollowerId, followerId)
                .eq(SysFollow::getFolloweeId, followeeId);
        return followMapper.selectCount(wrapper) > 0;
    }

    /**
     * 粉丝数
     */
    @Override
    public int countFollowers(Long userId) {
        LambdaQueryWrapper<SysFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFollow::getFolloweeId, userId);
        return followMapper.selectCount(wrapper).intValue();
    }

    /**
     * 关注数
     */
    @Override
    public int countFollowing(Long userId) {
        LambdaQueryWrapper<SysFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFollow::getFollowerId, userId);
        return followMapper.selectCount(wrapper).intValue();
    }

    /**
     * 粉丝列表
     */
    @Override
    public Page<FollowUserVO> pageFollowers(Long userId, Integer pageNum, Integer pageSize) {
        Page<SysFollow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFollow::getFolloweeId, userId).orderByDesc(SysFollow::getCreateTime);
        Page<SysFollow> followPage = followMapper.selectPage(page, wrapper);
        List<Long> ids = followPage.getRecords().stream()
                .map(SysFollow::getFollowerId)
                .toList();
        return buildUserPage(followPage, ids);
    }

    /**
     * 关注列表
     */
    @Override
    public Page<FollowUserVO> pageFollowing(Long userId, Integer pageNum, Integer pageSize) {
        Page<SysFollow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFollow::getFollowerId, userId).orderByDesc(SysFollow::getCreateTime);
        Page<SysFollow> followPage = followMapper.selectPage(page, wrapper);
        List<Long> ids = followPage.getRecords().stream()
                .map(SysFollow::getFolloweeId)
                .toList();
        return buildUserPage(followPage, ids);
    }

    /**
     * 组装关注用户分页数据
     */
    private Page<FollowUserVO> buildUserPage(Page<SysFollow> followPage, List<Long> ids) {
        Page<FollowUserVO> voPage = new Page<>(followPage.getCurrent(), followPage.getSize(), followPage.getTotal());
        if (ids == null || ids.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 批量查询用户信息
        List<SysUser> users = sysUserMapper.selectBatchIds(ids);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        Set<Long> followedByMe = new HashSet<>();
        if (StpUtil.isLogin()) {
            Long me = StpUtil.getLoginIdAsLong();
            // 查询当前登录人是否也关注了这些用户
            List<SysFollow> myFollows = followMapper.selectList(
                    new LambdaQueryWrapper<SysFollow>()
                            .eq(SysFollow::getFollowerId, me)
                            .in(SysFollow::getFolloweeId, ids)
            );
            followedByMe.addAll(myFollows.stream().map(SysFollow::getFolloweeId).collect(Collectors.toSet()));
        }

        // 按原顺序组装分页结果
        List<FollowUserVO> records = ids.stream().map(id -> {
            SysUser user = userMap.get(id);
            FollowUserVO vo = new FollowUserVO();
            vo.setId(id);
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            vo.setFollowed(followedByMe.contains(id));
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }
}
