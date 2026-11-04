import request from './request'

export const getDailyRecommendations = (limit = 20) => request.get('/recommend/daily', { params: { limit } })

export const getTasteAnalysis = () => request.get('/recommend/taste')

export const getSimilarSongs = (songId, limit = 10) => request.get(`/recommend/similar/${songId}`, { params: { limit } })
