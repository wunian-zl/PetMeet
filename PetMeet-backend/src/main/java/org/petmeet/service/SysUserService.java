package org.petmeet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.dto.LoginDTO;
import org.petmeet.dto.RegisterDTO;
import org.petmeet.entity.SysUser;
import org.petmeet.vo.LoginVO;
import org.petmeet.vo.UserInfoVO;

public interface SysUserService extends IService<SysUser> {
    LoginVO register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    void logout();

    UserInfoVO getCurrentUser();

    UserInfoVO getUserInfoById(Long userId);

    void updateUserInfo(SysUser user);

    /**
     * 管理员登录
     * 仅允许role=admin的用户登录
     */
    LoginVO adminLogin(LoginDTO dto);
}
