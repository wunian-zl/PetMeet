package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.entity.CmsNote;
import org.petmeet.entity.SysInteraction;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.CmsNoteMapper;
import org.petmeet.mapper.SysInteractionMapper;
import org.petmeet.service.AdminUserService;
import org.petmeet.service.SysNotificationService;
import org.petmeet.vo.AdminUserVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final OmsOrderMapper omsOrderMapper;
    private final CmsNoteMapper cmsNoteMapper;
    private final SysInteractionMapper sysInteractionMapper;
    private final SysNotificationService notificationService;

    /**
     * 用户列表
     */
    @Override
    public Page<AdminUserVO> pageList(Integer pageNum, Integer pageSize, String keyword, String role, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索(用户名、昵称、手机号)
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        // 角色筛选
        if (StrUtil.isNotBlank(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        // 状态筛选
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> userPage = sysUserMapper.selectPage(page, wrapper);

        // 转成视图对象
        Page<AdminUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 用户详情
     */
    @Override
    public AdminUserVO getDetail(Long id) {
        // 查询用户详情
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }
        return toVO(user);
    }

    /**
     * 新增用户
     */
    @Override
    public Long createUser(SysUser user) {
        // 检查用户名唯一
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw AppException.badRequest("用户名已存在");
        }

        // 管理员新增用户时必须显式提供符合要求的初始密码。
        String initialPassword = user.getPassword();
        if (StrUtil.isBlank(initialPassword)) {
            throw AppException.badRequest("初始密码不能为空");
        }
        if (initialPassword.length() < 8
                || initialPassword.length() > 64
                || !initialPassword.matches(".*[A-Za-z].*")
                || !initialPassword.matches(".*\\d.*")) {
            throw AppException.badRequest("初始密码必须为8-64位，且同时包含字母和数字");
        }
        user.setPassword(BCrypt.hashpw(initialPassword));
        if (user.getRole() == null) {
            user.setRole("user");
        }
        if (user.getStatus() == null) {
            user.setStatus(SysUser.STATUS_ENABLED);
        }
        user.setCreateTime(LocalDateTime.now());

        sysUserMapper.insert(user);
        return user.getId();
    }

    /**
     * 修改用户
     */
    @Override
    public void updateUser(SysUser user) {
        // 查询原用户
        SysUser existing = sysUserMapper.selectById(user.getId());
        if (existing == null) {
            throw AppException.notFound("用户不存在");
        }

        // 不允许修改密码(使用重置密码接口)
        // 不允许通过管理端修改登录账号(username)，避免触发唯一约束及账号混乱
        user.setPassword(null);
        user.setUsername(null);

        Long currentId = StpUtil.getLoginIdAsLong();
        boolean isSelfAdmin = currentId != null
                && currentId.equals(user.getId())
                && "admin".equals(existing.getRole());

        // 管理员编辑自己的管理员账号时，只允许改手机号、邮箱和头像。
        if (!isSelfAdmin) {
            user.setPhone(null);
            user.setEmail(null);
            user.setAvatar(null);
        }
        sysUserMapper.updateById(user);
    }

    /**
     * 封禁用户
     */
    @Override
    public void banUser(Long id, String reason) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }

        user.setStatus(SysUser.STATUS_DISABLED);
        user.setBanReason(reason);
        user.setBanTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    /**
     * 解封用户
     */
    @Override
    public void unbanUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }

        user.setStatus(SysUser.STATUS_ENABLED);
        user.setBanReason(null);
        user.setBanTime(null);
        sysUserMapper.updateById(user);
    }

    /**
     * 删除用户
     */
    @Override
    public void deleteUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }
        // 不允许删除管理员
        if ("admin".equals(user.getRole())) {
            throw AppException.badRequest("不能删除管理员账户");
        }
        sysUserMapper.deleteById(id);
    }

    /**
     * 重置密码
     */
    @Override
    public String resetPassword(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }

        // 生成随机密码
        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(BCrypt.hashpw(newPassword));
        sysUserMapper.updateById(user);

        return newPassword;
    }

    /**
     * 强制下线
     */
    @Override
    public void forceLogout(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }
        StpUtil.kickout(id);
    }

    /**
     * 和谐头像
     */
    @Override
    public void harmonizeAvatar(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }

        // MyBatis-Plus 默认会忽略 updateById() 里的 null 值。
        // 这里改用 UpdateWrapper，强制把 avatar 更新成 NULL。
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id).set("avatar", null);
        sysUserMapper.update(null, updateWrapper);

        // 通知用户重新上传头像。
        notificationService.sendToUser(
                id,
                "头像已被和谐",
                "你的头像涉嫌违规，平台已将其清空，请尽快更换头像后再继续使用相关功能。",
                "profile",
                id
        );
    }

    /**
     * 转换后台用户数据
     */
    private AdminUserVO toVO(SysUser user) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtil.copyProperties(user, vo);

        // 查询统计数据
        try {
            Long noteCount = cmsNoteMapper.selectCount(
                    new LambdaQueryWrapper<CmsNote>()
                            .eq(CmsNote::getUserId, user.getId()));
            vo.setNoteCount(noteCount.intValue());
        } catch (Exception e) {
            vo.setNoteCount(0);
        }

        try {
            Long orderCount = omsOrderMapper.selectCount(
                    new LambdaQueryWrapper<org.petmeet.entity.OmsOrder>()
                            .eq(org.petmeet.entity.OmsOrder::getUserId, user.getId()));
            vo.setOrderCount(orderCount.intValue());
        } catch (Exception e) {
            vo.setOrderCount(0);
        }

        // 消费总额需要聚合查询，暂时先返回0
        vo.setTotalSpent(BigDecimal.ZERO);

        // 笔记获赞总数
        try {
            Object sumObj = cmsNoteMapper.selectObjs(
                    new QueryWrapper<CmsNote>()
                            .select("COALESCE(SUM(like_count), 0)")
                            .eq("user_id", user.getId()))
                    .stream().findFirst().orElse(null);
            int totalLikes = sumObj == null ? 0 : ((Number) sumObj).intValue();
            vo.setTotalLikeCount(totalLikes);
        } catch (Exception e) {
            vo.setTotalLikeCount(0);
        }

        // 用户收藏笔记数
        try {
            Long collectCount = sysInteractionMapper.selectCount(
                    new LambdaQueryWrapper<SysInteraction>()
                            .eq(SysInteraction::getUserId, user.getId())
                            .eq(SysInteraction::getType, SysInteraction.TYPE_COLLECT_NOTE));
            vo.setCollectNoteCount(collectCount.intValue());
        } catch (Exception e) {
            vo.setCollectNoteCount(0);
        }

        return vo;
    }
}
