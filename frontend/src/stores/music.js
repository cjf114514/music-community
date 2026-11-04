import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useMusicStore = defineStore('music', () => {
  const savedState = (() => {
    try {
      const raw = localStorage.getItem('music_player_state')
      return raw ? JSON.parse(raw) : null
    } catch { return null }
  })()

  const currentSong = ref(savedState?.currentSong || null)
  const isPlaying = ref(false)
  const playList = ref(savedState?.playList || [])
  const queue = ref(savedState?.queue || [])
  const isQueueMode = ref(savedState?.isQueueMode || false)
  const volume = ref(savedState?.volume ?? 80)
  const currentTime = ref(0)
  const duration = ref(0)
  const playMode = ref(savedState?.playMode || 'list')
  const showPlayList = ref(false)
  const currentSongIndex = ref(-1)
  const showDesktopLyrics = ref(false)
  const seekTarget = ref(null)

  function saveState() {
    try {
      const state = {
        currentSong: currentSong.value,
        playList: playList.value,
        queue: queue.value,
        isQueueMode: isQueueMode.value,
        volume: volume.value,
        playMode: playMode.value
      }
      localStorage.setItem('music_player_state', JSON.stringify(state))
    } catch {}
  }

  watch([currentSong, playList, queue, isQueueMode, volume, playMode], saveState, { deep: true })

  function setCurrentSong(song) {
    currentSong.value = song
    isPlaying.value = true
    currentTime.value = 0
    duration.value = 0
  }

  function setPlayList(list) {
    playList.value = list
  }

  function togglePlay() {
    isPlaying.value = !isPlaying.value
  }

  function setVolume(val) {
    volume.value = val
  }

  function setCurrentTime(time) {
    currentTime.value = time
  }

  function setDuration(time) {
    duration.value = time
  }

  function seekTo(time) {
    currentTime.value = time
    seekTarget.value = time
  }

  function setPlayMode(mode) {
    playMode.value = mode
  }

  function togglePlayList() {
    showPlayList.value = !showPlayList.value
  }

  function closePlayList() {
    showPlayList.value = false
  }

  function removeFromPlayList(index) {
    playList.value.splice(index, 1)
  }

  function addToQueue(song) {
    const exists = queue.value.some(s => s.id === song.id)
    if (exists) return false
    queue.value.push(song)
    return true
  }

  function removeFromQueue(index) {
    queue.value.splice(index, 1)
  }

  function clearQueue() {
    queue.value = []
  }

  function toggleQueueMode() {
    isQueueMode.value = !isQueueMode.value
  }

  function reorderQueue(fromIndex, toIndex) {
    const item = queue.value.splice(fromIndex, 1)[0]
    queue.value.splice(toIndex, 0, item)
  }

  function clearPlayList() {
    playList.value = []
    currentSong.value = null
    isPlaying.value = false
  }

  function getCurrentList() {
    return isQueueMode.value ? queue.value : playList.value
  }

  function playNext() {
    const list = getCurrentList()
    if (list.length === 0) return
    const currentIdx = list.findIndex(s => s.id === currentSong.value?.id)
    let nextIdx
    if (playMode.value === 'random') {
      nextIdx = Math.floor(Math.random() * list.length)
    } else if (currentIdx < list.length - 1) {
      nextIdx = currentIdx + 1
    } else {
      nextIdx = 0
    }
    setCurrentSong(list[nextIdx])
  }

  function playPrev() {
    const list = getCurrentList()
    if (list.length === 0) return
    const currentIdx = list.findIndex(s => s.id === currentSong.value?.id)
    let nextIdx
    if (playMode.value === 'random') {
      nextIdx = Math.floor(Math.random() * list.length)
    } else if (currentIdx > 0) {
      nextIdx = currentIdx - 1
    } else {
      nextIdx = list.length - 1
    }
    setCurrentSong(list[nextIdx])
  }

  return {
    currentSong,
    isPlaying,
    playList,
    queue,
    isQueueMode,
    volume,
    currentTime,
    duration,
    playMode,
    showPlayList,
    currentSongIndex,
    showDesktopLyrics,
    seekTarget,
    setCurrentSong,
    setPlayList,
    togglePlay,
    setVolume,
    setCurrentTime,
    setDuration,
    seekTo,
    setPlayMode,
    togglePlayList,
    closePlayList,
    removeFromPlayList,
    addToQueue,
    removeFromQueue,
    clearQueue,
    toggleQueueMode,
    reorderQueue,
    clearPlayList,
    getCurrentList,
    playNext,
    playPrev
  }
})
