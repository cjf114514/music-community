import request from './request'

/** 查看音乐库配置（目录、是否存在、是否启用） */
export const getLibraryConfig = () => request.get('/admin/library/config')

/**
 * 扫描本地音乐库
 * @param {boolean} dryRun true=只统计不写入
 *
 * 注意这里把超时放宽到 3 分钟：扫描要遍历整个目录并逐个解析 ID3，
 * 几百首歌可能超过 request.js 默认的 10 秒。
 */
export const scanLibrary = (dryRun = false) =>
  request.post('/admin/library/scan', null, {
    params: { dryRun },
    timeout: 180000
  })
