import request from './request'

export const getNotifications = () => request.get('/notifications')

export const getUnreadCount = () => request.get('/notifications/unread-count')

export const markAllAsRead = () => request.post('/notifications/read-all')

export const markAsRead = (id) => request.post(`/notifications/${id}/read`)
