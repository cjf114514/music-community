import request from './request'

export const getSongComments = (songId, sortBy = 'time') => request.get(`/comments/song/${songId}`, { params: { sortBy } })

export const addComment = (songId, data) => request.post(`/comments/song/${songId}`, data)

export const deleteComment = (commentId) => request.delete(`/comments/${commentId}`)

export const toggleLike = (commentId) => request.post(`/comments/${commentId}/like`)

export const getPlaylistComments = (playlistId, sortBy = 'time') => request.get(`/comments/playlist/${playlistId}`, { params: { sortBy } })

export const addPlaylistComment = (playlistId, data) => request.post(`/comments/playlist/${playlistId}`, data)

export const getSongDanmaku = (songId) => request.get(`/comments/song/${songId}/danmaku`)
