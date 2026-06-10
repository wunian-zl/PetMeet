package org.petmeet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.entity.CmsBanner;

import java.util.List;

public interface CmsBannerService extends IService<CmsBanner> {
    List<CmsBanner> listActiveByPosition(String position);

    /**
     * 归一化“近N天”跳转参数：当 recentDaysUntil 已过期时，自动移除 recentDays/recentDaysUntil，等价于“不限制”。
     * 该方法会在必要时更新数据库中的 linkUrl。
     */
    void normalizeExpiredRecentDays(List<CmsBanner> banners);
}
