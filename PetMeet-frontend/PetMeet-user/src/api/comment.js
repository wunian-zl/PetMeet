import request from '@/utils/request'

export function getCommentList(params) {
  return request.get('/comment/list', { params })
}

export function getCommentReplies(id, params) {
  return request.get(`/comment/${id}/replies`, { params })
}

export function addComment(data) {
  return request.post('/comment/add', data)
}

export function deleteComment(id) {
  return request.delete(`/comment/${id}`)
}

export function toggleCommentLike(id) {
  return request.post(`/comment/${id}/like`)
}
