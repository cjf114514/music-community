import request from './request'

export const recordPlay = (songId) => request.post('/play-history/record', { songId })

export const getRecentPlays = (limit = 50) => request.get('/play-history/recent', { params: { limit } })

export const clearHistory = () => request.delete('/play-history/clear')

export const getRanking = (type = 'total', limit = 20) => request.get('/play-history/ranking', { params: { type, limit } })
