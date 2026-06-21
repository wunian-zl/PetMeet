import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const request = axios.create({
    baseURL: '/api',
    timeout: 5000
})

const fallbackMessageByCode = {
    400: '请求参数有误',
    403: '没有操作权限',
    404: '请求的资源不存在',
    409: '状态已变化,请刷新后重试',
    500: '系统繁忙,请稍后再试'
}

const buildBizError = (res) => {
    const code = res?.code
    const message = res?.msg || fallbackMessageByCode[code] || '请求失败'
    const error = new Error(message)
    error.code = code
    error.response = res
    error.__biz = true
    return error
}

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
            return Promise.reject(buildBizError(res))
        }

        const err = buildBizError(res)
        if (!response.config?.silentError) {
            ElMessage.error(err.message)
        }
        return Promise.reject(err)
    },
    (error) => {
        if (error && error.__biz) {
            return Promise.reject(error)
        }
        const status = error.response?.status
        const data = error.response?.data
        const message = data?.msg || data?.message || fallbackMessageByCode[status] || error.message || '请求失败'
        if (!error.config?.silentError) {
            ElMessage.error(message)
        }
        return Promise.reject(error)
    }
)

export default request
