import request from './request'

export const getConversations = () => request.get('/messages/conversations')

export const getOrCreateConversation = (userId) => request.get(`/messages/conversation/${userId}`)

export const getMessages = (conversationId, page = 0, size = 20) =>
  request.get(`/messages/${conversationId}`, { params: { page, size } })

export const sendMessage = (receiverId, content) =>
  request.post('/messages/send', { receiverId, content })

export const markConversationRead = (conversationId) =>
  request.post(`/messages/${conversationId}/read`)

export const getMessageUnreadCount = () => request.get('/messages/unread-count')

export const recallMessage = (messageId) =>
  request.post(`/messages/${messageId}/recall`)

export const canSendMessage = (receiverId) =>
  request.get(`/messages/can-send/${receiverId}`)
