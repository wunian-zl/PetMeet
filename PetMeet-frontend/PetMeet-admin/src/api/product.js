import request from './request'

/**
 * 获取商品列表
 */
export function getProductList(params) {
    return request({
        url: '/admin/product/list',
        method: 'get',
        params
    })
}

/**
 * 获取商品详情
 */
export function getProductDetail(id) {
    return request({
        url: `/admin/product/${id}`,
        method: 'get'
    })
}

/**
 * 创建商品
 */
export function createProduct(data) {
    return request({
        url: '/admin/product',
        method: 'post',
        data
    })
}

/**
 * 更新商品
 */
export function updateProduct(id, data) {
    return request({
        url: `/admin/product/${id}`,
        method: 'put',
        data
    })
}

/**
 * 上架/下架商品
 */
export function changeProductStatus(id, status) {
    return request({
        url: `/admin/product/${id}/status`,
        method: 'put',
        params: { status }
    })
}

/**
 * 删除商品
 */
export function deleteProduct(id) {
    return request({
        url: `/admin/product/${id}`,
        method: 'delete'
    })
}

/**
 * 批量操作
 */
export function batchProductAction(action, ids) {
    return request({
        url: '/admin/product/batch',
        method: 'post',
        params: { action },
        data: ids
    })
}


/**
 * 获取商品分类列表
 */
export function getCategoryList() {
    return request({
        url: '/category/list/all',
        method: 'get'
    })
}

/**
 * 创建分类
 */
export function createCategory(data) {
    return request({
        url: '/category/add',
        method: 'post',
        data
    })
}

/**
 * 更新分类
 */
export function updateCategory(data) {
    return request({
        url: '/category/update',
        method: 'post',
        data
    })
}

/**
 * 删除分类
 */
export function deleteCategory(id) {
    return request({
        url: `/category/delete/${id}`,
        method: 'delete'
    })
}
