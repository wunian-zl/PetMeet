import request from './request'

/**
 * 获取统计卡片数据
 */
export function getStats(range = 'today') {
    return request({
        url: '/admin/dashboard/stats',
        method: 'get',
        params: { range }
    })
}

/**
 * 获取趋势图数据
 */
export function getTrend(range = 'week') {
    return request({
        url: '/admin/dashboard/trend',
        method: 'get',
        params: { range }
    })
}

/**
 * 获取分类销售占比
 */
export function getCategorySales() {
    return request({
        url: '/admin/dashboard/category-sales',
        method: 'get'
    })
}

/**
 * 获取热门商品TOP5
 */
export function getTopProducts() {
    return request({
        url: '/admin/dashboard/top-products',
        method: 'get'
    })
}

/**
 * 获取待办事项
 */
export function getTodos() {
    return request({
        url: '/admin/dashboard/todos',
        method: 'get'
    })
}
