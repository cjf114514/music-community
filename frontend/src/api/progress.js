import request from './request'

/**
 * 上报播放位置
 * 位置小于 5 秒视为「从头开始 / 已播完」，后端会清掉该歌的进度
 */
export const saveProgress = (songId, position) =>
  request.put('/progress', { songId, position })

/** 查询某首歌上次听到哪里 */
export const getProgress = (songId) => request.get(`/progress/${songId}`)

/** 最近一次没播完的歌（打开网站时用于「继续上次播放」） */
export const getResume = () => request.get('/progress/resume')

/** 清除某首歌的进度 */
export const clearProgress = (songId) => request.delete(`/progress/${songId}`)
