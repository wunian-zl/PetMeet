import request from '@/utils/request'

export function createPay(data) {
  return request.post('/pay/create', data)
}

export function getPayStatus(paySn, params, config = {}) {
  return request.get(`/pay/status/${paySn}`, { params, ...config })
}

export function mockConfirmPay(paySn) {
  return request.post(`/pay/mock/confirm/${paySn}`)
}
