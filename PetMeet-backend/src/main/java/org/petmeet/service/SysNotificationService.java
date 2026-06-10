package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.entity.SysNotification;

import java.util.List;

public interface SysNotificationService extends IService<SysNotification> {

    Page<SysNotification> pageMy(Integer pageNum, Integer pageSize, Integer unreadOnly);

    Integer unreadCount();

    void markRead(Long id);

    void markAllRead();

    void deleteMy(Long id);

    void deleteMyBatch(List<Long> ids);

    void sendToUser(Long userId, String title, String content, String bizType, Long bizId);
}
