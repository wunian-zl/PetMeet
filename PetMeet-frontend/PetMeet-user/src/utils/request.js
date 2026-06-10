import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const request = axios.create({
    baseURL: '/api',
    timeout: 5000
})

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        // 从本地读取 token
        const token = localStorage.getItem('token')

        // 把 token 放到请求头中，后端据此识别当前登录用户
        if (token) {
            config.headers['Authorization'] = token
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    (response) => {
        const res = response.data

        // 按项目约定，接口统一走 { code, msg, data } 结构
        if (res.code === 200) {
            return res.data
        }

        // 处理 401 未授权
        if (res.code === 401) {
            const userStore = useUserStore()

            // 先清空本地登录态
            userStore.logout()

            // 弹出登录框，提醒用户重新登录
            userStore.showLogin()
            return Promise.reject(new Error(res.msg || 'Unauthorized'))
        }

        // 处理其他错误
        ElMessage.error(res.msg || 'Error')
        return Promise.reject(new Error(res.msg || 'Error'))
    },
    (error) => {
        ElMessage.error(error.message || 'Request Error')
        return Promise.reject(error)
    }
)

export default request
