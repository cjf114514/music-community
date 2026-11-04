import request from './request'

export const recordSearch = (keyword) => request.post('/search/history', { keyword })

export const getSearchHistory = (limit = 10) => request.get('/search/history', { params: { limit } })

export const deleteSearchHistory = (keyword) => request.delete('/search/history', { params: { keyword } })

export const clearSearchHistory = () => request.delete('/search/history/clear')

export const getHotSearch = (limit = 10) => request.get('/search/hot', { params: { limit } })
