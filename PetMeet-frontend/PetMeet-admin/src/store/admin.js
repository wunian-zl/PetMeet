import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/auth'

export const useAdminStore = defineStore('admin', {
    state: () => ({
        token: localStorage.getItem('adminToken') || '',
        userInfo: JSON.parse(localStorage.getItem('adminUserInfo') || '{}')
    }),
    getters: {
        isLoggedIn: (state) => !!state.token
    },
    actions: {
        async login(username, password) {
            try {
                const res = await loginApi({ username, password })
                if (res.code === 200 && res.data) {
                    this.token = res.data.token
                    this.userInfo = {
                        userId: res.data.userId,
                        username: res.data.username,
                        nickname: res.data.nickname,
                        avatar: res.data.avatar
                    }
                    localStorage.setItem('adminToken', this.token)
                    localStorage.setItem('adminUserInfo', JSON.stringify(this.userInfo))
                    return { success: true }
                }
                return { success: false, message: res.message || '登录失败' }
            } catch (error) {
                return { success: false, message: error.message || '登录失败' }
            }
        },
        async logout() {
            try {
                await logoutApi()
            } catch (e) {
                // 即使API调用失败也清除本地状态
            }
            this.token = ''
            this.userInfo = {}
            localStorage.removeItem('adminToken')
            localStorage.removeItem('adminUserInfo')
        }
    }
})
