package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.petmeet.entity.SysUser;
import org.petmeet.vo.AdminUserVO;

/**
 * 管理端用户服务接口
 *
 * @author zjx
 */
public interface AdminUserService {

    /**
     * 分页查询用户列表
     */
    Page<AdminUserVO> pageList(Integer pageNum, Integer pageSize, String keyword, String role, Integer status);

    /**
     * 获取用户详情
     */
    AdminUserVO getDetail(Long id);

    /**
     * 创建用户
     */
    Long createUser(SysUser user);

    /**
     * 更新用户
     */
    void updateUser(SysUser user);

    /**
     * 封禁用户
     */
    void banUser(Long id, String reason);

    /**
     * 解封用户
     */
    void unbanUser(Long id);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 重置密码
     */
    String resetPassword(Long id);

    /**
     * 强制下线
     */
    void forceLogout(Long id);

    void harmonizeAvatar(Long id);
}
