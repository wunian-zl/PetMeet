import request from '@/utils/request'

export function submitComplaint(data) {
  return request.post('/complaint', data)
}

export function getMyComplaintList(params) {
  return request.get('/complaint/my/list', { params })
}

export function getMyComplaintDetail(id) {
  return request.get(`/complaint/my/${id}`)
}

export function deleteMyComplaint(id) {
  return request.delete(`/complaint/my/${id}`)
}

export function getMyLatestComplaintByNote(noteId) {
  return request.get('/complaint/my/latest', { params: { noteId } })
}

export function feedbackComplaint(id, data) {
  return request.put(`/complaint/${id}/feedback`, data)
}
