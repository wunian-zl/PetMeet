package org.petmeet;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.petmeet.common.AppException;
import org.petmeet.dto.ChangePasswordDTO;
import org.petmeet.dto.LoginDTO;
import org.petmeet.dto.ResetPasswordDTO;
import org.petmeet.dto.RegisterDTO;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.SysUserService;
import org.petmeet.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class SysUserPasswordServiceTests {

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysUserMapper userMapper;

    private Long loginId;

    @AfterEach
    void clearLoginState() {
        if (loginId != null) {
            StpUtil.kickout(loginId);
        }
    }

    @Test
    void registerReturnsLoginTokenAfterBindingContact() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("register_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        dto.setPassword("PetMeet2026");
        dto.setNickname("register test");
        dto.setEmail("register-test@example.com");

        LoginVO vo = userService.register(dto);
        loginId = vo.getUserId();

        assertTrue(vo.getToken() != null && !vo.getToken().isBlank());
    }

    @Test
    void loginUnknownUserDoesNotAutoRegister() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("missing_" + UUID.randomUUID().toString().replace("-", ""));
        dto.setPassword("PetMeet2026");

        assertThrows(AppException.class, () -> userService.login(dto));
    }

    @Test
    void resetPasswordRequiresMatchingBoundContact() {
        SysUser user = createUser("PetMeet2026", "13800138000", "reset@example.com");

        ResetPasswordDTO wrongContact = new ResetPasswordDTO();
        wrongContact.setUsername(user.getUsername());
        wrongContact.setContact("13900139000");
        wrongContact.setNewPassword("PetMeet2027");

        assertThrows(AppException.class, () -> userService.resetPassword(wrongContact));

        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setUsername(user.getUsername());
        dto.setContact("reset@example.com");
        dto.setNewPassword("PetMeet2027");

        userService.resetPassword(dto);

        SysUser updated = userMapper.selectById(user.getId());
        assertTrue(BCrypt.checkpw("PetMeet2027", updated.getPassword()));
    }

    @Test
    void changePasswordRequiresOldPassword() {
        SysUser user = createUser("PetMeet2026", "13800138001", "change@example.com");
        loginId = user.getId();
        StpUtil.login(loginId);

        ChangePasswordDTO wrongOldPassword = new ChangePasswordDTO();
        wrongOldPassword.setOldPassword("Wrong2026");
        wrongOldPassword.setNewPassword("PetMeet2027");

        assertThrows(AppException.class, () -> userService.changePassword(wrongOldPassword));

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("PetMeet2026");
        dto.setNewPassword("PetMeet2027");

        userService.changePassword(dto);

        SysUser updated = userMapper.selectById(user.getId());
        assertTrue(BCrypt.checkpw("PetMeet2027", updated.getPassword()));
    }

    private SysUser createUser(String password, String phone, String email) {
        SysUser user = new SysUser();
        user.setUsername("password_test_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPassword(BCrypt.hashpw(password));
        user.setNickname("password test");
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(SysUser.ROLE_USER);
        user.setStatus(SysUser.STATUS_ENABLED);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }
}
