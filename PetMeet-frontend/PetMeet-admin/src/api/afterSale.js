import request from './request'

export const getAfterSaleList = (params) => request({
  url: '/admin/after-sale/list',
  method: 'get',
  params
})

export const getAfterSaleDetail = (id) =>
  request({
    url: `/admin/after-sale/${id}`,
    method: 'get'
  })

export const updateAfterSaleStatus = (id, status, remark) =>
  request({
    url: `/admin/after-sale/${id}/status`,
    method: 'put',
    params: { status, remark }
  })

export const approveRefund = (id, data = {}) =>
  request({
    url: `/admin/after-sale/${id}/approve-refund`,
    method: 'post',
    data
  })

export const approveReturn = (id, data = {}) =>
  request({
    url: `/admin/after-sale/${id}/approve-return`,
    method: 'post',
    data
  })

export const confirmReturnRefund = (id, data = {}) =>
  request({
    url: `/admin/after-sale/${id}/confirm-return-refund`,
    method: 'post',
    data
  })

export const confirmReturnExchange = (id, data = {}) =>
  request({
    url: `/admin/after-sale/${id}/confirm-return-exchange`,
    method: 'post',
    data
  })

export const shipExchange = (id, data = {}) =>
  request({
    url: `/admin/after-sale/${id}/ship-exchange`,
    method: 'post',
    data
  })

export const rejectAfterSale = (id, data = {}) =>
  request({
    url: `/admin/after-sale/${id}/reject`,
    method: 'post',
    data
  })

export const deleteAfterSale = (id) =>
  request({
    url: `/admin/after-sale/${id}`,
    method: 'delete'
  })

export const batchDeleteAfterSale = (ids) =>
  request({
    url: '/admin/after-sale/batch-delete',
    method: 'post',
    data: ids
  })
