package org.petmeet.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/admin/**")
                        .notMatch("/admin/auth/login")
                        .check(route -> {
                            StpUtil.checkLogin();
                            StpUtil.checkRole("admin");
                        })))
                .addPathPatterns("/**")
                .order(-100);
        registry.addInterceptor(trafficInterceptor).addPathPatterns("/**");
    }
}
