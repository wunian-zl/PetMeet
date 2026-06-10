package org.petmeet.support;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }

        SysUser user;
        try {
            user = userMapper.selectById(Long.valueOf(loginId.toString()));
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }

        if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || user.getRole() == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(user.getRole());
    }
}
