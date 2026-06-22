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

const DEFAULT_IMAGE_PLACEHOLDER = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22300%22 height=%22375%22 viewBox=%220 0 300 375%22%3E%3Crect width=%22300%22 height=%22375%22 fill=%22%23f2f1ee%22/%3E%3Cg fill=%22none%22 stroke=%22%23c9c6bf%22 stroke-width=%228%22 stroke-linecap=%22round%22 stroke-linejoin=%22round%22 opacity=%220.9%22%3E%3Cpath d=%22M103 201c10-28 35-47 58-47s48 19 58 47%22/%3E%3Cpath d=%22M113 218c24 18 72 18 96 0%22/%3E%3Ccircle cx=%22118%22 cy=%22130%22 r=%2213%22 fill=%22%23c9c6bf%22 stroke=%22none%22/%3E%3Ccircle cx=%22155%22 cy=%22112%22 r=%2215%22 fill=%22%23c9c6bf%22 stroke=%22none%22/%3E%3Ccircle cx=%22192%22 cy=%22130%22 r=%2213%22 fill=%22%23c9c6bf%22 stroke=%22none%22/%3E%3C/g%3E%3C/svg%3E'

/**
 * 处理图片 URL，确保路径正确
 * - 如果是完整 URL (http/https)，直接返回
 * - 如果是相对路径，拼接后端地址
 * - 如果是空值，返回默认占位图
 * @param {string} url - 原始图片路径
 * @param {string} placeholder - 默认占位图
 * @returns {string} 处理后的完整 URL
 */
export const getImageUrl = (url, placeholder = DEFAULT_IMAGE_PLACEHOLDER) => {
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
