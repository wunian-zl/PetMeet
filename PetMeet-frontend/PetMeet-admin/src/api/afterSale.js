import request from './request'

export const getAfterSaleList = (params) => request.get('/admin/after-sale/list', { params })

export const updateAfterSaleStatus = (id, status, remark) =>
  request.put(`/admin/after-sale/${id}/status`, null, { params: { status, remark } })

export const deleteAfterSale = (id) =>
  request.delete(`/admin/after-sale/${id}`)

export const batchDeleteAfterSale = (ids) =>
  request.post('/admin/after-sale/batch-delete', ids)
