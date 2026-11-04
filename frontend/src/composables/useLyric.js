import { computed, ref } from 'vue'
import { useMusicStore } from '@/stores/music'

export function useLyric(lyricContent) {
  const store = useMusicStore()
  const offset = ref(parseFloat(localStorage.getItem('lyric-offset') || '0'))

  const parsedLyrics = computed(() => {
    if (!lyricContent.value) return []
    const lines = lyricContent.value.split('\n')
    const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/
    const grouped = new Map()

    for (const line of lines) {
      const match = line.match(timeRegex)
      if (!match) continue
      const minutes = parseInt(match[1])
      const seconds = parseInt(match[2])
      const milliseconds = parseInt(match[3].padEnd(3, '0'))
      const rawTime = minutes * 60 + seconds + milliseconds / 1000
      const text = match[4].trim()
      if (!text) continue

      const key = rawTime.toFixed(3)
      if (!grouped.has(key)) {
        grouped.set(key, { time: rawTime, text, translation: '' })
      } else {
        grouped.get(key).translation = text
      }
    }

    const result = []
    for (const entry of grouped.values()) {
      result.push({
        time: entry.time + offset.value,
        text: entry.text,
        translation: entry.translation
      })
    }
    return result
  })

  const currentLyricIndex = computed(() => {
    if (!parsedLyrics.value.length) return -1
    const currentTime = store.currentTime
    let index = -1
    for (let i = 0; i < parsedLyrics.value.length; i++) {
      if (parsedLyrics.value[i].time <= currentTime) {
        index = i
      } else {
        break
      }
    }
    return index
  })

  const currentLyricText = computed(() => {
    const idx = currentLyricIndex.value
    if (idx < 0 || !parsedLyrics.value[idx]) return ''
    return parsedLyrics.value[idx].text
  })

  const seekToLine = (time) => {
    if (time == null || isNaN(time)) return
    store.seekTo(time)
  }

  const setOffset = (val) => {
    offset.value = val
    localStorage.setItem('lyric-offset', String(val))
  }

  const adjustOffset = (delta) => {
    const newVal = Math.max(-5, Math.min(5, parseFloat((offset.value + delta).toFixed(1))))
    setOffset(newVal)
  }

  return {
    parsedLyrics,
    currentLyricIndex,
    currentLyricText,
    seekToLine,
    offset,
    setOffset,
    adjustOffset
  }
}
