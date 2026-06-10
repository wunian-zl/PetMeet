import request from './request'

/**
 * 获取用户列表
 */
export function getUserList(params) {
    return request({
        url: '/admin/user/list',
        method: 'get',
        params
    })
}

/**
 * 获取用户详情
 */
export function getUserDetail(id) {
    return request({
        url: `/admin/user/${id}`,
        method: 'get'
    })
}

/**
 * 创建用户
 */
export function createUser(data) {
    return request({
        url: '/admin/user',
        method: 'post',
        data
    })
}

/**
 * 更新用户
 */
export function updateUser(id, data) {
    return request({
        url: `/admin/user/${id}`,
        method: 'put',
        data
    })
}

/**
 * 封禁用户
 */
export function banUser(id, reason) {
    return request({
        url: `/admin/user/${id}/ban`,
        method: 'put',
        params: { reason }
    })
}

/**
 * 解封用户
 */
export function unbanUser(id) {
    return request({
        url: `/admin/user/${id}/unban`,
        method: 'put'
    })
}

/**
 * 删除用户
 */
export function deleteUser(id) {
    return request({
        url: `/admin/user/${id}`,
        method: 'delete'
    })
}

/**
 * 重置密码
 */
export function resetPassword(id) {
    return request({
        url: `/admin/user/${id}/reset-password`,
        method: 'post'
    })
}

/**
 * 强制下线
 */
export function forceLogout(id) {
    return request({
        url: `/admin/user/${id}/force-logout`,
        method: 'post'
    })
}

export function harmonizeAvatar(id) {
    return request({
        url: `/admin/user/${id}/harmonize-avatar`,
        method: 'post'
    })
}
