package org.petmeet.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    private static final String PV_KEY_PREFIX = "traffic:pv:";
    private static final Duration PV_TTL = Duration.ofDays(180);
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!shouldCount(request, response)) {
            return;
        }
        String key = PV_KEY_PREFIX + LocalDate.now().format(DATE);
        try {
            Long v = redisTemplate.opsForValue().increment(key);
            if (v != null && v == 1L) {
                redisTemplate.expire(key, PV_TTL);
            }
        } catch (Exception e) {
            log.debug("pv increment failed", e);
        }
    }

    private boolean shouldCount(HttpServletRequest request, HttpServletResponse response) {
        if (request == null || response == null) {
            return false;
        }
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        int status = response.getStatus();
        if (status >= 400) {
            return false;
        }

        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        // 不把管理端接口计入站点访问量。
        if (uri.startsWith("/admin/")) {
            return false;
        }
        // 跳过文档接口和静态资源。
        if (uri.startsWith("/swagger") || uri.startsWith("/v3/api-docs") || uri.startsWith("/error") || uri.startsWith("/images/")) {
            return false;
        }

        // 只统计面向用户的页面型接口，避免把每次 AJAX 请求都算进 PV。
        return "/note/list".equals(uri)
                || uri.startsWith("/note/detail/")
                || "/product/list".equals(uri)
                || uri.startsWith("/product/detail/");
    }
}
