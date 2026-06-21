package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.petmeet.entity.SysNotification;
import org.petmeet.mapper.SysNotificationMapper;
import org.petmeet.service.SysNotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysNotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification> implements SysNotificationService {

    private final SysNotificationMapper notificationMapper;

    /**
     * 我的通知列表
     */
    @Override
    public Page<SysNotification> pageMy(Integer pageNum, Integer pageSize, Integer unreadOnly) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<SysNotification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getUserId, userId);
        if (unreadOnly != null && unreadOnly == 1) {
            wrapper.eq(SysNotification::getIsRead, 0);
        }
        wrapper.orderByDesc(SysNotification::getCreateTime);
        notificationMapper.selectPage(page, wrapper);
        return page;
    }

    /**
     * 未读数量
     */
    @Override
    public Integer unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0)
        ).intValue();
    }

    /**
     * 标记已读
     */
    @Override
    public void markRead(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        int updated = notificationMapper.update(
                null,
                new LambdaUpdateWrapper<SysNotification>()
                        .eq(SysNotification::getId, id)
                        .eq(SysNotification::getUserId, userId)
                        .set(SysNotification::getIsRead, 1)
                        .set(SysNotification::getReadTime, LocalDateTime.now())
        );
        if (updated <= 0) {
            throw AppException.notFound("通知不存在");
        }
    }

    /**
     * 全部已读
     */
    @Override
    public void markAllRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationMapper.update(
                null,
                new LambdaUpdateWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0)
                        .set(SysNotification::getIsRead, 1)
                        .set(SysNotification::getReadTime, LocalDateTime.now())
        );
    }

    /**
     * 删除通知
     */
    @Override
    public void deleteMy(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        int deleted = notificationMapper.delete(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getId, id)
                        .eq(SysNotification::getUserId, userId)
        );
        if (deleted <= 0) {
            throw AppException.notFound("通知不存在");
        }
    }

    /**
     * 批量删除通知
     */
    @Override
    public void deleteMyBatch(List<Long> ids) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> normalizedIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            return;
        }

        notificationMapper.delete(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .in(SysNotification::getId, normalizedIds)
        );
    }

    /**
     * 发送通知
     */
    @Override
    public void sendToUser(Long userId, String title, String content, String bizType, Long bizId) {
        if (userId == null) return;
        if (StrUtil.isBlank(title)) return;

        // 组装通知数据并保存
        SysNotification n = new SysNotification();
        n.setUserId(userId);
        n.setTitle(title.trim());
        n.setContent(StrUtil.trimToNull(content));
        n.setBizType(StrUtil.trimToNull(bizType));
        n.setBizId(bizId);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }
}
