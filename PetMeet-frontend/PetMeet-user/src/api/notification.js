import request from '@/utils/request'

export function getNotificationList(params) {
  return request.get('/notification/list', { params })
}

export function getNotificationUnreadCount() {
  return request.get('/notification/unread-count')
}

export function markNotificationRead(id) {
  return request.put(`/notification/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.put('/notification/read-all')
}

export function deleteNotification(id) {
  return request.delete(`/notification/${id}`)
}

export function deleteNotifications(ids) {
  return request.post('/notification/batch-delete', ids)
}
