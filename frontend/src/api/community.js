import request from './request'

export const createPost = (data) => request.post('/community/post', data)
export const getPosts = (limit = 20) => request.get('/community/posts', { params: { limit } })
export const getUserPosts = (userId, limit = 20) => request.get(`/community/posts/user/${userId}`, { params: { limit } })
export const getPost = (postId) => request.get(`/community/posts/${postId}`)
export const updatePost = (postId, data) => request.put(`/community/posts/${postId}`, data)
export const deletePost = (postId) => request.delete(`/community/posts/${postId}`)
export const likePost = (postId) => request.post(`/community/posts/${postId}/like`)
export const getLikeStatus = (postId) => request.get(`/community/posts/${postId}/like-status`)
export const createComment = (postId, data) => request.post(`/community/posts/${postId}/comment`, data)
export const getPostComments = (postId, sortBy = 'time') => request.get(`/community/posts/${postId}/comments`, { params: { sortBy } })
export const likeComment = (commentId) => request.post(`/community/comments/${commentId}/like`)
export const getCommentReplies = (postId, parentId) => request.get(`/community/posts/${postId}/comments/${parentId}/replies`)
export const deleteComment = (commentId) => request.delete(`/community/comments/${commentId}`)
export const searchPosts = (keyword) => request.get('/community/search', { params: { keyword } })
export const uploadPostImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/post-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
