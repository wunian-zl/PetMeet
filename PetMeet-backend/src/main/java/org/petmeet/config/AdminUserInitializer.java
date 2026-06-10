package org.petmeet.config;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.entity.SysUser;
import org.petmeet.service.SysUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final SysUserService sysUserService;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.nickname:管理员}")
    private String adminNickname;

    @Override
    public void run(String... args) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, adminUsername);
        if (sysUserService.count(wrapper) > 0) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "首次创建管理员时必须配置环境变量PETMEET_ADMIN_PASSWORD");
        }

        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPassword(BCrypt.hashpw(adminPassword));
        admin.setNickname(adminNickname);
        admin.setRole("admin");
        admin.setStatus(1);
        sysUserService.save(admin);

        log.info("Default admin user created: {}", adminUsername);
    }
}
