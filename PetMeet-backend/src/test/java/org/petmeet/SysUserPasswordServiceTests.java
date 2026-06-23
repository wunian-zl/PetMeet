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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        dto.setUsername("reg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        dto.setPassword("PetMeet2026");
        dto.setNickname("register test");
        dto.setEmail("register-test@example.com");

        LoginVO vo = userService.register(dto);
        loginId = vo.getUserId();

        assertTrue(vo.getToken() != null && !vo.getToken().isBlank());
    }

    @Test
    void registerRejectsReservedUsername() {
        RegisterDTO dto = createRegisterDto("admin");

        AppException exception = assertThrows(AppException.class, () -> userService.register(dto));

        assertEquals(400, exception.getCode());
        assertEquals("该用户名不可使用，请换一个", exception.getMessage());
    }

    @Test
    void registerRejectsReservedUsernameCaseInsensitive() {
        RegisterDTO dto = createRegisterDto("Admin");

        AppException exception = assertThrows(AppException.class, () -> userService.register(dto));

        assertEquals(400, exception.getCode());
        assertEquals("该用户名不可使用，请换一个", exception.getMessage());
    }

    @Test
    void registerRejectsWeakPasswordAtServiceLayer() {
        RegisterDTO dto = createRegisterDto("weak_password_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6));
        dto.setPassword("ab");

        AppException exception = assertThrows(AppException.class, () -> userService.register(dto));

        assertEquals(400, exception.getCode());
        assertEquals("密码必须为8-18位，且同时包含字母和数字", exception.getMessage());
    }

    @Test
    void registerRejectsLongPasswordAtServiceLayer() {
        RegisterDTO dto = createRegisterDto("long_password_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6));
        dto.setPassword("PetMeet2026Password");

        AppException exception = assertThrows(AppException.class, () -> userService.register(dto));

        assertEquals(400, exception.getCode());
        assertEquals("密码必须为8-18位，且同时包含字母和数字", exception.getMessage());
    }

    @Test
    void registerRejectsChineseLoginUsernameAtServiceLayer() {
        RegisterDTO dto = createRegisterDto("猫咪用户");

        AppException exception = assertThrows(AppException.class, () -> userService.register(dto));

        assertEquals(400, exception.getCode());
        assertEquals("用户名需为2-20位，仅支持字母、数字或下划线", exception.getMessage());
    }

    @Test
    void loginUnknownUserDoesNotAutoRegister() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("missing_" + UUID.randomUUID().toString().replace("-", ""));
        dto.setPassword("PetMeet2026");

        assertThrows(AppException.class, () -> userService.login(dto));
    }

    @Test
    void adminCannotLoginFromUserSite() {
        SysUser admin = createUser("PetMeet2026", null, "admin-user-site@example.com", SysUser.ROLE_ADMIN);
        LoginDTO dto = new LoginDTO();
        dto.setUsername(admin.getUsername());
        dto.setPassword("PetMeet2026");

        AppException exception = assertThrows(AppException.class, () -> userService.login(dto));

        assertEquals(403, exception.getCode());
        assertEquals("管理员账号请从后台登录", exception.getMessage());
    }

    @Test
    void adminCanStillLoginFromAdminSite() {
        SysUser admin = createUser("PetMeet2026", null, "admin-site@example.com", SysUser.ROLE_ADMIN);
        LoginDTO dto = new LoginDTO();
        dto.setUsername(admin.getUsername());
        dto.setPassword("PetMeet2026");

        LoginVO vo = userService.adminLogin(dto);
        loginId = vo.getUserId();

        assertTrue(vo.getToken() != null && !vo.getToken().isBlank());
    }

    @Test
    void adminTokenCannotReadUserSiteProfile() {
        SysUser admin = createUser("PetMeet2026", null, "admin-profile@example.com", SysUser.ROLE_ADMIN);
        loginId = admin.getId();
        StpUtil.login(loginId);

        AppException exception = assertThrows(AppException.class, () -> userService.getCurrentUser());

        assertEquals(401, exception.getCode());
        assertEquals("请使用用户端账号登录", exception.getMessage());
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
        return createUser(password, phone, email, SysUser.ROLE_USER);
    }

    private SysUser createUser(String password, String phone, String email, String role) {
        SysUser user = new SysUser();
        user.setUsername("password_test_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPassword(BCrypt.hashpw(password));
        user.setNickname("password test");
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(SysUser.STATUS_ENABLED);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private RegisterDTO createRegisterDto(String username) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword("PetMeet2026");
        dto.setNickname(username);
        dto.setEmail(username.toLowerCase() + "-blocked@example.com");
        return dto;
    }
}
