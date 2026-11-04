import request from './request'

export const getAllSingers = () => request.get('/singers')

export const getSingerByName = (name) => request.get(`/singers/${name}`)

export const getSingerSongs = (name) => request.get(`/singers/${name}/songs`)
