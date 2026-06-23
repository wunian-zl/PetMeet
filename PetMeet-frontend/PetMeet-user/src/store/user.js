import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
    state: () => ({
        token: localStorage.getItem('token') || '',
        userInfo: {
            id: '',
            nickname: '',
            avatar: ''
        },
        loginVisible: false,  // Global modal visibility
        loginRedirect: '',
        cartCount: 0,         // 购物车数量
        unpaidOrderCount: 0,  // 待支付订单数量
        notificationUnreadCount: 0 // 未读通知数量
    }),
    getters: {
        // 判断是否已登录
        isLoggedIn: (state) => !!state.token
    },
    actions: {
        // 登录弹窗控制
        showLogin(redirect = '') {
            this.loginRedirect = redirect || ''
            this.loginVisible = true
        },
        hideLogin() {
            this.loginVisible = false
        },

        // 登录功能
        async login(loginForm, options = {}) {
            // 调用后端登录接口
            const res = await request.post('/auth/login', loginForm, options)

            await this.applyLoginResult(res)
        },

        async register(registerForm) {
            const res = await request.post('/auth/register', registerForm)
            await this.applyLoginResult(res)
        },

        async applyLoginResult(res) {
            // 把 token 保存到 Pinia 和 localStorage
            const token = res.token || res
            this.token = token
            localStorage.setItem('token', token)

            // 登录成功后刷新页面上要用到的用户相关数据
            await this.getUserInfo()
            await this.fetchCartCount() // 登录后获取购物车数量
            await this.fetchUnpaidOrderCount() // 登录后获取待支付订单数量
            await this.fetchNotificationUnreadCount() // 登录后获取通知数量
        },

        // 获取当前登录用户信息
        async getUserInfo() {
            const res = await request.get('/user/info')
            this.userInfo = res
        },

        // 退出登录
        logout() {
            // 清空本地 token 和用户相关状态
            this.token = ''
            this.userInfo = { id: '', nickname: '', avatar: '' }
            this.cartCount = 0
            this.unpaidOrderCount = 0
            this.notificationUnreadCount = 0
            localStorage.removeItem('token')
        },

        // 获取购物车数量
        async fetchCartCount() {
            if (!this.token) {
                this.cartCount = 0
                return
            }
            try {
                const res = await request.get('/cart/count')
                this.cartCount = res || 0
            } catch (error) {
                this.cartCount = 0
            }
        },

        // 获取待支付订单数量
        async fetchUnpaidOrderCount() {
            if (!this.token) {
                this.unpaidOrderCount = 0
                return
            }
            try {
                const res = await request.get('/order/list', { params: { pageNum: 1, pageSize: 1, status: 0 } })
                this.unpaidOrderCount = res?.total || 0
            } catch (error) {
                this.unpaidOrderCount = 0
            }
        },

        // 获取未读通知数量
        async fetchNotificationUnreadCount() {
            if (!this.token) {
                this.notificationUnreadCount = 0
                return
            }
            try {
                const res = await request.get('/notification/unread-count')
                this.notificationUnreadCount = res || 0
            } catch (error) {
                this.notificationUnreadCount = 0
            }
        },

        // 更新购物车数量（加入购物车后调用）
        updateCartCount(count) {
            this.cartCount = count
        },

        // 增加购物车数量
        incrementCartCount(delta = 1) {
            this.cartCount += delta
        }
    }
})

