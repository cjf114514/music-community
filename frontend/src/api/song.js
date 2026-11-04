import request from './request'

export const getAllSongs = () => request.get('/songs')

export const getRandomSongs = (limit) => request.get('/songs/random', { params: { limit } })

export const searchSongs = (keyword) => request.get('/songs/search', { params: { keyword } })

export const getSongById = (id) => request.get(`/songs/${id}`)

export const getSongLyric = (id) => request.get(`/songs/${id}/lyric`)

export const uploadAudio = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/audio', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const uploadCover = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/cover', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const uploadLyric = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/lyric', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updateSong = (id, data) => request.put(`/songs/${id}`, data)

export const updateSongDuration = (id, duration) => request.patch(`/songs/${id}/duration`, { duration })

export const getAudioDuration = (file) => {
  return new Promise((resolve) => {
    const audio = new Audio()
    const url = URL.createObjectURL(file)
    audio.onloadedmetadata = () => {
      URL.revokeObjectURL(url)
      resolve(Math.round(audio.duration))
    }
    audio.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(null)
    }
    audio.src = url
  })
}

export const fillMissingDurations = (songs) => {
  const missing = songs.filter(s => !s.duration || s.duration === 0)
  if (!missing.length) return
  missing.forEach(song => {
    if (!song.url) return
    const audio = new Audio()
    audio.preload = 'metadata'
    audio.onloadedmetadata = () => {
      const dur = Math.round(audio.duration)
      if (dur > 0) {
        song.duration = dur
        updateSongDuration(song.id, dur).catch(() => {})
      }
    }
    audio.onerror = () => {}
    audio.src = song.url
  })
}

export const getPublicPlaylists = (category) => {
  const params = category ? { category } : {}
  return request.get('/playlists', { params })
}

export const getPlaylistCategories = () => request.get('/playlists/categories')

export const getUserPlaylists = (userId) => request.get(`/playlists/user/${userId}`)

export const getPlaylistById = (id) => request.get(`/playlists/${id}`)

export const getPlaylistSongs = (id) => request.get(`/playlists/${id}/songs`)

export const createPlaylist = (data) => request.post('/playlists', data)

export const updatePlaylist = (id, data) => request.put(`/playlists/${id}`, data)

export const deletePlaylist = (id) => request.delete(`/playlists/${id}`)

export const addSongToPlaylist = (playlistId, songId) => request.post(`/playlists/${playlistId}/songs/${songId}`)

export const removeSongFromPlaylist = (playlistId, songId) => request.delete(`/playlists/${playlistId}/songs/${songId}`)

export const searchPlaylists = (keyword) => request.get('/playlists/search', { params: { keyword } })

export const reorderPlaylistSongs = (playlistId, songIds) => request.put(`/playlists/${playlistId}/songs/reorder`, { songIds })

export const searchUsers = (keyword, page = 0, size = 20) => request.get('/search/users', { params: { keyword, page, size } })

export const searchSingers = (keyword) => request.get('/singers/search', { params: { keyword } })

export const downloadSong = (id) => {
  const token = localStorage.getItem('token')
  const url = `/api/songs/${id}/download?token=${token}`
  const link = document.createElement('a')
  link.href = url
  link.download = ''
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
