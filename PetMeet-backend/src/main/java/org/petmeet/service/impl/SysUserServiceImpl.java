package org.petmeet.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.dto.LoginDTO;
import org.petmeet.dto.RegisterDTO;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.SysUserService;
import org.petmeet.vo.LoginVO;
import org.petmeet.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.nickname:管理员}")
    private String adminNickname;

    @Value("${app.admin.auto-create:true}")
    private Boolean adminAutoCreate;

    /**
     * 注册功能
     */
    @Override
    public LoginVO register(RegisterDTO dto) {
        // 根据用户名查询用户是否已存在
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 封装用户对象，并对密码进行加密后保存
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : username);
        user.setPhone(dto.getPhone());
        user.setRole("user");
        user.setStatus(1);
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
            throw new RuntimeException("用户名不能为空");
        }
        String loginPassword = dto.getPassword() == null ? "" : dto.getPassword();

        // 根据用户名查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginUsername);
        SysUser user = this.getOne(wrapper);

        // 用户不存在时，按项目规则自动注册一个普通用户
        if (user == null) {
            validateAutoRegisterCredentials(loginUsername, loginPassword);
            user = createAutoRegisteredUser(loginUsername, loginPassword);
            log.info("Auto registered user on login: username={}", loginUsername);
        }

        // 校验密码和账号状态
        if (!BCrypt.checkpw(loginPassword, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
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
        return getUserInfoById(userId);
    }

    @Override
    public UserInfoVO getUserInfoById(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    /**
     * 修改个人资料
     */
    @Override
    public void updateUserInfo(SysUser user) {
        // 只允许修改当前登录用户自己的资料
        Long userId = StpUtil.getLoginIdAsLong();
        user.setId(userId);

        // 这些敏感字段不允许在这里直接修改
        user.setPassword(null);
        user.setUsername(null);
        this.updateById(user);
    }

    /**
     * 管理员登录
     */
    @Override
    public LoginVO adminLogin(LoginDTO dto) {
        String loginUsername = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (loginUsername.isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }

        // 根据用户名查询管理员用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginUsername);
        SysUser user = this.getOne(wrapper);

        // 管理员账号不存在时，按配置决定是否自动创建
        if (user == null) {
            String adminUsernameValue = adminUsername == null ? "" : adminUsername.trim();
            boolean allowAutoCreate = Boolean.TRUE.equals(adminAutoCreate)
                    && (adminUsernameValue.isEmpty() || adminUsernameValue.equalsIgnoreCase(loginUsername));
            if (allowAutoCreate) {
                SysUser admin = new SysUser();
                admin.setUsername(loginUsername);
                admin.setPassword(BCrypt.hashpw(dto.getPassword()));
                admin.setNickname(adminNickname);
                admin.setRole("admin");
                admin.setStatus(1);
                admin.setCreateTime(LocalDateTime.now());
                this.save(admin);
                user = admin;
            } else {
                throw new RuntimeException("用户不存在");
            }
        }

        // 校验密码、状态和角色
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!"admin".equals(user.getRole())) {
            throw new RuntimeException("无管理员权限");
        }

        // 创建管理员登录态，并更新最后登录时间
        StpUtil.login(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        return buildLoginVO(user);
    }

    /**
     * 自动注册前的参数校验
     */
    private void validateAutoRegisterCredentials(String username, String password) {
        if (username.length() < 2 || username.length() > 20 || !username.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$")) {
            throw new RuntimeException("账号未注册，自动注册要求用户名为2-20位，仅支持汉字、字母、数字或下划线");
        }
        if (password.length() < 8
                || password.length() > 64
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new RuntimeException("账号未注册，自动注册要求密码为8-64位，且同时包含字母和数字");
        }
    }

    /**
     * 自动注册用户
     */
    private SysUser createAutoRegisteredUser(String username, String password) {
        // 封装自动注册的普通用户信息
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setNickname(username);
        user.setRole("user");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        this.save(user);
        return user;
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
