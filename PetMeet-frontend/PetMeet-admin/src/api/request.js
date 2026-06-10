import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

let lastErrorAt = 0
let lastErrorMessage = ''

const showError = (message) => {
    const now = Date.now()
    if (message === lastErrorMessage && now - lastErrorAt < 2000) {
        return
    }
    lastErrorAt = now
    lastErrorMessage = message
    ElMessage.error(message)
}

// 创建axios实例
const request = axios.create({
    baseURL: '/api', // API的base_url
    timeout: 15000 // 请求超时时间
})

// 请求拦截器
request.interceptors.request.use(
    config => {
        // 从 localStorage 里取 token
        const token = localStorage.getItem('adminToken')
        if (token) {
            config.headers['Authorization'] = token
        }
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data
        // 返回码不是 200 时，按业务错误处理
        if (res.code && res.code !== 200) {
            const message = res.message || res.msg || '请求失败'
            showError(message)

            // 401：token 过期或未登录
            if (res.code === 401) {
                localStorage.removeItem('adminToken')
                router.push('/admin/login')
            }

            const err = new Error(message)
            // 标记成“业务错误”，避免被错误拦截器当成“网络错误”重复弹窗
            err.__biz = true
            return Promise.reject(err)
        }
        return res
    },
    error => {
        console.error('响应错误:', error)

        // 业务错误上面已经提示过一次，这里直接透传
        if (error && error.__biz) {
            return Promise.reject(error)
        }

        if (error.response) {
            const responseMessage = error.response.data?.message || error.response.data?.msg
            const silent404 = error.config?.silent404 === true
            if (silent404 && error.response.status === 404) {
                return Promise.reject(error)
            }
            switch (error.response.status) {
                case 401:
                    showError(responseMessage || '登录已过期,请重新登录')
                    localStorage.removeItem('adminToken')
                    router.push('/admin/login')
                    break
                case 403:
                    showError(responseMessage || '没有权限访问')
                    break
                case 404:
                    showError(responseMessage || '请求的资源不存在')
                    break
                case 500:
                    showError(responseMessage || '服务器错误')
                    break
                default:
                    showError(responseMessage || error.message || '请求失败')
            }
        } else {
            showError('网络错误,请检查网络连接')
        }

        return Promise.reject(error)
    }
)

export default request
