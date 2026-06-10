import request from './request'

/**
 * 管理员登录
 */
export function login(data) {
    return request({
        url: '/admin/auth/login',
        method: 'post',
        data
    })
}

/**
 * 管理员登出
 */
export function logout() {
    return request({
        url: '/admin/auth/logout',
        method: 'post'
    })
}
