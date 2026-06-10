import request from './request'

/**
 * 获取广告位列表
 */
export function getBannerList(params) {
  return request({
    url: '/admin/banner/list',
    method: 'get',
    params
  })
}

/**
 * 新增广告位
 */
export function createBanner(data) {
  return request({
    url: '/admin/banner',
    method: 'post',
    data
  })
}

/**
 * 更新广告位
 */
export function updateBanner(id, data) {
  return request({
    url: `/admin/banner/${id}`,
    method: 'put',
    data
  })
}

/**
 * 启用/禁用广告位
 */
export function changeBannerStatus(id, status) {
  return request({
    url: `/admin/banner/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 删除广告位
 */
export function deleteBanner(id) {
  return request({
    url: `/admin/banner/${id}`,
    method: 'delete'
  })
}

