package org.petmeet.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.petmeet.support.TrafficInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class TrafficWebMvcConfig implements WebMvcConfigurer {

    private final TrafficInterceptor trafficInterceptor;
    private static final String[] USER_SITE_PRIVATE_PATHS = {
            "/user/**",
            "/cart/**",
            "/order/**",
            "/after-sale/**",
            "/complaint/**",
            "/notification/**",
            "/pay/create",
            "/pay/status/**",
            "/pay/mock/**",
            "/comment/add",
            "/comment/*/like",
            "/follow/*",
            "/follow/status/**",
            "/note/publish",
            "/note/my/**",
            "/note/like/**",
            "/note/collect/**",
            "/note/recommend/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    SaRouter.match("/admin/**")
                            .notMatch("/admin/auth/login")
                            .check(route -> {
                                StpUtil.checkLogin();
                                StpUtil.checkRole("admin");
                            });
                    SaRouter.match(USER_SITE_PRIVATE_PATHS)
                            .check(route -> {
                                StpUtil.checkLogin();
                                checkUserSiteRole();
                            });
                    SaRouter.match(SaHttpMethod.DELETE)
                            .match("/comment/*")
                            .check(route -> {
                                StpUtil.checkLogin();
                                checkUserSiteRole();
                            });
                }))
                .addPathPatterns("/**")
                .order(-100);
        registry.addInterceptor(trafficInterceptor).addPathPatterns("/**");
    }

    private void checkUserSiteRole() {
        if (StpUtil.hasRole("admin")) {
            StpUtil.logout();
            throw org.petmeet.common.AppException.unauthorized("请使用用户端账号登录");
        }
    }
}
