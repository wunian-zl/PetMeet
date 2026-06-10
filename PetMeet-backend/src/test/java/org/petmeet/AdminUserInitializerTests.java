package org.petmeet;

import org.junit.jupiter.api.Test;
import org.petmeet.config.AdminUserInitializer;
import org.petmeet.entity.SysUser;
import org.petmeet.service.SysUserService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserInitializerTests {

    @Test
    void existingAdminDoesNotRequireInitialPassword() {
        SysUserService userService = mock(SysUserService.class);
        when(userService.count(any())).thenReturn(1L);
        AdminUserInitializer initializer = createInitializer(userService, "");

        initializer.run();

        verify(userService, never()).save(any(SysUser.class));
    }

    @Test
    void missingAdminRequiresInitialPassword() {
        SysUserService userService = mock(SysUserService.class);
        when(userService.count(any())).thenReturn(0L);
        AdminUserInitializer initializer = createInitializer(userService, "");

        assertThrows(IllegalStateException.class, initializer::run);
        verify(userService, never()).save(any(SysUser.class));
    }

    private AdminUserInitializer createInitializer(SysUserService userService, String password) {
        AdminUserInitializer initializer = new AdminUserInitializer(userService);
        ReflectionTestUtils.setField(initializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "adminPassword", password);
        ReflectionTestUtils.setField(initializer, "adminNickname", "管理员");
        return initializer;
    }
}
