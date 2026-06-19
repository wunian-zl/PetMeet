/**
 * 图片 URL 处理工具函数
 */

// 后端基础地址
const normalizeBaseUrl = (value) => {
    if (typeof value !== 'string') {
        return ''
    }
    return value.trim().replace(/\/$/, '')
}

const BASE_URL = normalizeBaseUrl(import.meta.env.VITE_API_ORIGIN)

/**
 * 处理图片 URL，确保路径正确
 * - 如果是完整 URL (http/https)，直接返回
 * - 如果是相对路径，拼接后端地址
 * - 如果是空值，返回默认占位图
 * @param {string} url - 原始图片路径
 * @param {string} placeholder - 默认占位图
 * @returns {string} 处理后的完整 URL
 */
export const getImageUrl = (url, placeholder = 'https://picsum.photos/300/300') => {
    if (!url) {
        return placeholder
    }

    // 如果已经是完整 URL，直接返回
    if (url.startsWith('http://') || url.startsWith('https://')) {
        return url
    }

    // 如果是 /images 开头的路径，拼接后端地址
    if (url.startsWith('/images/')) {
        return BASE_URL + url
    }

    // 如果是其他相对路径（如 /files/），也拼接后端地址
    if (url.startsWith('/')) {
        return BASE_URL + url
    }

    // 其他情况保留原样
    return url
}

/**
 * 处理头像 URL
 */
export const getAvatarUrl = (url) => {
    return getImageUrl(url, 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')
}

/**
 * 批量处理图片数组
 */
export const getImageUrls = (urls) => {
    if (!urls || !Array.isArray(urls)) {
        return []
    }
    return urls.map(url => getImageUrl(url))
}
