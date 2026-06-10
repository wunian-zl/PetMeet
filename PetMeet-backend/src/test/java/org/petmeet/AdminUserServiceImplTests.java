package org.petmeet;

import cn.dev33.satoken.secure.BCrypt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.CmsNoteMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.SysInteractionMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.SysNotificationService;
import org.petmeet.service.impl.AdminUserServiceImpl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTests {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private OmsOrderMapper omsOrderMapper;

    @Mock
    private CmsNoteMapper cmsNoteMapper;

    @Mock
    private SysInteractionMapper sysInteractionMapper;

    @Mock
    private SysNotificationService notificationService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void createUserRejectsMissingPassword() {
        SysUser user = newUser(null);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        assertThrows(RuntimeException.class, () -> adminUserService.createUser(user));
        verify(sysUserMapper, never()).insert(any());
    }

    @Test
    void createUserRejectsWeakPassword() {
        SysUser user = newUser("abcdefgh");
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        assertThrows(RuntimeException.class, () -> adminUserService.createUser(user));
        verify(sysUserMapper, never()).insert(any());
    }

    @Test
    void createUserHashesValidPassword() {
        SysUser user = newUser("PetMeet2026");
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        adminUserService.createUser(user);

        assertTrue(BCrypt.checkpw("PetMeet2026", user.getPassword()));
        verify(sysUserMapper).insert(user);
    }

    private SysUser newUser(String password) {
        SysUser user = new SysUser();
        user.setUsername("new_user");
        user.setPassword(password);
        return user;
    }
}
