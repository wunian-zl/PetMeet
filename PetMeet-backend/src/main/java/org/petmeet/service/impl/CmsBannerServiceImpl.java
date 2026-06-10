package org.petmeet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.petmeet.entity.CmsBanner;
import org.petmeet.mapper.CmsBannerMapper;
import org.petmeet.service.CmsBannerService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CmsBannerServiceImpl extends ServiceImpl<CmsBannerMapper, CmsBanner> implements CmsBannerService {

    /**
     * 查询有效轮播图
     */
    @Override
    public List<CmsBanner> listActiveByPosition(String position) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<CmsBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsBanner::getStatus, 1)
                .eq(CmsBanner::getPosition, position)
                .and(q -> q.isNull(CmsBanner::getStartTime)
                        .or()
                        .le(CmsBanner::getStartTime, now))
                .and(q -> q.isNull(CmsBanner::getEndTime)
                        .or()
                        .ge(CmsBanner::getEndTime, now))
                .orderByAsc(CmsBanner::getSort, CmsBanner::getId);
        List<CmsBanner> list = this.list(wrapper);
        normalizeExpiredRecentDays(list);
        return list;
    }

    /**
     * 规范最近几天参数
     */
    @Override
    public void normalizeExpiredRecentDays(List<CmsBanner> banners) {
        if (banners == null || banners.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (CmsBanner banner : banners) {
            if (banner == null || banner.getId() == null) {
                continue;
            }
            String linkUrl = banner.getLinkUrl();
            if (linkUrl == null || !linkUrl.contains("recentDaysUntil")) {
                continue;
            }

            String untilStr = getQueryParam(linkUrl, "recentDaysUntil");
            if (untilStr == null || untilStr.isBlank()) {
                continue;
            }

            LocalDate until;
            try {
                until = LocalDate.parse(untilStr.trim());
            } catch (Exception ignored) {
                continue;
            }

            if (!today.isAfter(until)) {
                continue;
            }

            String normalized = removeQueryParams(linkUrl, "recentDays", "recentDaysUntil");
            if (Objects.equals(normalized, linkUrl)) {
                continue;
            }

            // 更新返回对象
            banner.setLinkUrl(normalized);
            // 持久化：避免管理端一直看到旧配置
            CmsBanner upd = new CmsBanner();
            upd.setId(banner.getId());
            upd.setLinkUrl(normalized);
            this.updateById(upd);
        }
    }

    /**
     * 读取链接参数
     */
    private String getQueryParam(String url, String key) {
        if (url == null || key == null || key.isBlank()) {
            return null;
        }
        int idx = url.indexOf('?');
        if (idx < 0 || idx == url.length() - 1) {
            return null;
        }
        String qs = url.substring(idx + 1);
        String[] pairs = qs.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isBlank()) continue;
            int eq = pair.indexOf('=');
            String k = eq >= 0 ? pair.substring(0, eq) : pair;
            if (!key.equals(k)) continue;
            return eq >= 0 ? pair.substring(eq + 1) : "";
        }
        return null;
    }

    /**
     * 移除链接参数
     */
    private String removeQueryParams(String url, String... keys) {
        if (url == null) return null;
        int idx = url.indexOf('?');
        if (idx < 0) return url;
        String path = url.substring(0, idx);
        String qs = idx == url.length() - 1 ? "" : url.substring(idx + 1);
        if (qs.isBlank()) return path;

        java.util.Set<String> remove = new java.util.HashSet<>();
        if (keys != null) {
            for (String k : keys) {
                if (k != null && !k.isBlank()) remove.add(k);
            }
        }

        java.util.List<String> kept = new java.util.ArrayList<>();
        for (String pair : qs.split("&")) {
            if (pair == null || pair.isBlank()) continue;
            int eq = pair.indexOf('=');
            String k = eq >= 0 ? pair.substring(0, eq) : pair;
            if (remove.contains(k)) continue;
            kept.add(pair);
        }

        if (kept.isEmpty()) {
            return path;
        }
        return path + "?" + String.join("&", kept);
    }
}
