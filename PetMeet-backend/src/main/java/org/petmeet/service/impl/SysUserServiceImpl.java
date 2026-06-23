package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.dto.ChangePasswordDTO;
import org.petmeet.dto.LoginDTO;
import org.petmeet.dto.RegisterDTO;
import org.petmeet.dto.ResetPasswordDTO;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.SysUserService;
import org.petmeet.vo.LoginVO;
import org.petmeet.vo.UserInfoVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private static final String PASSWORD_POLICY_MESSAGE = "密码必须为8-18位，且同时包含字母和数字";
    private static final String USERNAME_POLICY_MESSAGE = "用户名需为2-20位，仅支持字母、数字或下划线";

    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin",
            "administrator",
            "root",
            "system",
            "official",
            "petmeet",
            "客服",
            "管理员",
            "系统"
    );

    /**
     * 注册功能
     */
    @Override
    public LoginVO register(RegisterDTO dto) {
        // 根据用户名查询用户是否已存在
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        String email = dto.getEmail() == null ? "" : dto.getEmail().trim();
        ensureValidUsername(username);
        ensureStrongPassword(dto.getPassword());
        if (isReservedUsername(username)) {
            throw AppException.badRequest("该用户名不可使用，请换一个");
        }
        if (phone.isEmpty() && email.isEmpty()) {
            throw AppException.badRequest("请至少填写手机号或邮箱，用于找回密码");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (this.count(wrapper) > 0) {
            throw AppException.badRequest("用户名已存在");
        }

        // 封装用户对象，并对密码进行加密后保存
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : username);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setEmail(email.isEmpty() ? null : email);
        user.setRole(SysUser.ROLE_USER);
        user.setStatus(SysUser.STATUS_ENABLED);
        user.setCreateTime(LocalDateTime.now());
        this.save(user);

        // 注册成功后直接创建登录态，并记录最后登录时间
        StpUtil.login(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        // 返回前端需要的登录结果
        return buildLoginVO(user);
    }

    /**
     * 登录功能
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        String loginUsername = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (loginUsername.isEmpty()) {
            throw AppException.badRequest("用户名不能为空");
        }
        String loginPassword = dto.getPassword() == null ? "" : dto.getPassword();

        // 根据用户名查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginUsername);
        SysUser user = this.getOne(wrapper);

        if (user == null) {
            throw AppException.notFound("账号未注册");
        }

        // 校验密码和账号状态
        if (!BCrypt.checkpw(loginPassword, user.getPassword())) {
            throw AppException.badRequest("密码错误");
        }
        if (!Integer.valueOf(SysUser.STATUS_ENABLED).equals(user.getStatus())) {
            throw AppException.badRequest("账号已被禁用");
        }
        if (SysUser.ROLE_ADMIN.equals(user.getRole())) {
            throw AppException.forbidden("管理员账号请从后台登录");
        }

        // 创建登录态，并更新最后登录时间
        StpUtil.login(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        // 返回前端需要的 token 和用户信息
        return buildLoginVO(user);
    }

    /**
     * 登出功能
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前登录用户信息
     */
    @Override
    public UserInfoVO getCurrentUser() {
        // 从登录态中获取当前用户 id
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = this.getById(userId);
        ensureUserSiteAccount(user);
        return buildUserInfoVO(user);
    }

    @Override
    public UserInfoVO getUserInfoById(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }
        return buildUserInfoVO(user);
    }

    /**
     * 修改个人资料
     */
    @Override
    public void updateUserInfo(SysUser user) {
        // 只允许修改当前登录用户自己的资料
        Long userId = StpUtil.getLoginIdAsLong();
        ensureUserSiteAccount(this.getById(userId));
        user.setId(userId);

        // 这些敏感字段不允许在这里直接修改
        user.setPassword(null);
        user.setUsername(null);
        this.updateById(user);
    }

    /**
     * 找回密码：校验用户名和已绑定手机号/邮箱后重置密码。
     */
    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        String contact = dto.getContact() == null ? "" : dto.getContact().trim();
        ensureStrongPassword(dto.getNewPassword());
        if (username.isEmpty() || contact.isEmpty()) {
            throw AppException.badRequest("请填写用户名和已绑定的手机号或邮箱");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(wrapper);
        if (user == null || SysUser.ROLE_ADMIN.equals(user.getRole()) || !matchesBoundContact(user, contact)) {
            throw AppException.badRequest("账号或绑定信息不匹配");
        }
        if (!Integer.valueOf(SysUser.STATUS_ENABLED).equals(user.getStatus())) {
            throw AppException.badRequest("账号已被禁用");
        }

        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        this.updateById(user);
    }

    /**
     * 修改当前登录用户密码。
     */
    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = this.getById(userId);
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }
        ensureUserSiteAccount(user);
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw AppException.badRequest("原密码错误");
        }
        if (BCrypt.checkpw(dto.getNewPassword(), user.getPassword())) {
            throw AppException.badRequest("新密码不能和原密码相同");
        }
        ensureStrongPassword(dto.getNewPassword());

        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        this.updateById(user);
    }

    /**
     * 管理员登录
     */
    @Override
    public LoginVO adminLogin(LoginDTO dto) {
        String loginUsername = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (loginUsername.isEmpty()) {
            throw AppException.badRequest("用户名不能为空");
        }

        // 根据用户名查询管理员用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginUsername);
        SysUser user = this.getOne(wrapper);

        if (user == null) {
            throw AppException.notFound("管理员账号不存在");
        }

        // 校验密码、状态和角色
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw AppException.badRequest("密码错误");
        }
        if (!Integer.valueOf(SysUser.STATUS_ENABLED).equals(user.getStatus())) {
            throw AppException.badRequest("账号已被禁用");
        }
        if (!SysUser.ROLE_ADMIN.equals(user.getRole())) {
            throw AppException.forbidden("无管理员权限");
        }

        // 创建管理员登录态，并更新最后登录时间
        StpUtil.login(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        return buildLoginVO(user);
    }

    private boolean matchesBoundContact(SysUser user, String contact) {
        String phone = user.getPhone() == null ? "" : user.getPhone().trim();
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        return (!phone.isEmpty() && phone.equals(contact))
                || (!email.isEmpty() && email.equalsIgnoreCase(contact));
    }

    private boolean isReservedUsername(String username) {
        return RESERVED_USERNAMES.contains(username.toLowerCase(Locale.ROOT));
    }

    private void ensureValidUsername(String username) {
        if (username.length() < 2
                || username.length() > 20
                || !username.matches("^[A-Za-z0-9_]+$")) {
            throw AppException.badRequest(USERNAME_POLICY_MESSAGE);
        }
    }

    private void ensureStrongPassword(String password) {
        if (password == null
                || password.length() < 8
                || password.length() > 18
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw AppException.badRequest(PASSWORD_POLICY_MESSAGE);
        }
    }

    private void ensureUserSiteAccount(SysUser user) {
        if (user == null) {
            throw AppException.notFound("用户不存在");
        }
        if (SysUser.ROLE_ADMIN.equals(user.getRole())) {
            StpUtil.logout();
            throw AppException.unauthorized("请使用用户端账号登录");
        }
    }

    private UserInfoVO buildUserInfoVO(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    /**
     * 封装登录返回值
     */
    private LoginVO buildLoginVO(SysUser user) {
        // 把用户基础信息和 token 一起返回给前端
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setToken(StpUtil.getTokenValue());
        return vo;
    }
}
