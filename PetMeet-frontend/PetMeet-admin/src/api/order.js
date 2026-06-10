import request from './request'

/**
 * 获取订单列表
 */
export function getOrderList(params) {
    return request({
        url: '/admin/order/list',
        method: 'get',
        params
    })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id) {
    return request({
        url: `/admin/order/${id}`,
        method: 'get'
    })
}

/**
 * 发货
 */
export function shipOrder(id, shipInfo) {
    return request({
        url: `/admin/order/${id}/ship`,
        method: 'put',
        data: shipInfo
    })
}

/**
 * 退款处理
 */
export function refundOrder(id, refundInfo) {
    return request({
        url: `/admin/order/${id}/refund`,
        method: 'put',
        data: refundInfo
    })
}

/**
 * 取消订单
 */
export function cancelOrder(id) {
    return request({
        url: `/admin/order/${id}/cancel`,
        method: 'put'
    })
}

/**
 * 修改收货地址
 */
export function updateOrderAddress(id, addressInfo) {
    return request({
        url: `/admin/order/${id}/address`,
        method: 'put',
        data: addressInfo
    })
}

/**
 * 导出订单
 */
export function exportOrders(params) {
    return request({
        url: '/admin/order/export',
        method: 'get',
        params
    })
}

/**
 * 删除订单（管理端软删除）
 */
export function deleteOrder(id) {
    return request({
        url: `/admin/order/${id}`,
        method: 'delete'
    })
}

/**
 * 批量删除订单（管理端软删除）
 */
export function batchDeleteOrders(ids) {
    return request({
        url: '/admin/order/batch-delete',
        method: 'post',
        data: ids
    })
}
