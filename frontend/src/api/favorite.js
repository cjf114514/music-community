import request from './request'

export const toggleFavorite = (songId) => request.post('/favorites/toggle', { songId })

export const checkFavorite = (songId) => request.get(`/favorites/check/${songId}`)

export const getMyFavorites = () => request.get('/favorites/my')
