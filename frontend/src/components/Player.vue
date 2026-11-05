<template>
  <div class="player">
    <div v-if="showVisualizer && analyserNode" class="visualizer-bar">
      <AudioVisualizer :analyserNode="analyserNode" :width="visualizerWidth" :height="36" type="frequency" @toggle="showVisualizer = false" />
    </div>
    <div class="progress-bar" @click="handleProgressClick">
      <div class="progress-bg">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }">
          <div class="progress-dot"></div>
        </div>
      </div>
    </div>
    <div class="player-content">
      <div class="song-info">
        <div class="cover-wrapper" :class="{ playing: isPlaying }">
          <img v-if="currentSong?.coverUrl" :src="normalizeUrl(currentSong.coverUrl)" class="cover" />
          <div v-else class="cover-placeholder">
            <el-icon :size="36"><Music /></el-icon>
          </div>
        </div>
        <div class="song-detail" @click="showLyrics = true" style="cursor: pointer;">
          <div class="song-name">{{ currentSong?.name || '未播放' }}</div>
          <div class="song-singer">{{ currentSong?.singer || '未知歌手' }}</div>
          <div class="mini-lyric" v-if="currentSong && currentLyricText">{{ currentLyricText }}</div>
        </div>
        <el-button v-if="currentSong && userStore.isLoggedIn()" text @click="handleDownload" class="download-btn">
          <el-icon :size="20"><Download /></el-icon>
        </el-button>
      </div>

      <div class="controls">
        <div class="mode-btn" @click="togglePlayMode">
          <el-tooltip :content="modeText" placement="top">
            <span class="mode-icon" v-html="modeSvg"></span>
          </el-tooltip>
        </div>
        <el-button circle @click="playPrev" :disabled="currentList.length === 0">
          <el-icon><DArrowLeft /></el-icon>
        </el-button>
        <el-button circle type="primary" size="large" @click="togglePlay" :disabled="!currentSong">
          <el-icon :size="24" v-if="isPlaying"><VideoPause /></el-icon>
          <el-icon :size="24" v-else><VideoPlay /></el-icon>
        </el-button>
        <el-button circle @click="playNext" :disabled="currentList.length === 0">
          <el-icon><DArrowRight /></el-icon>
        </el-button>
        <el-button circle @click="store.togglePlayList">
          <el-icon><List /></el-icon>
        </el-button>
      </div>

      <div class="right-controls">
        <div class="time-display">
          <span>{{ formatTime(currentTime) }}</span>
          <span>/</span>
          <span>{{ formatTime(duration) }}</span>
        </div>
        <div class="speed-control" @click="showSpeedMenu = !showSpeedMenu" :class="{ active: playbackRate !== 1 }" title="倍速播放">
          <span class="speed-label">{{ playbackRate }}x</span>
        </div>
        <div class="desktop-lyrics-btn" @click="toggleDesktopLyrics" :class="{ active: showDesktopLyrics }" :title="showDesktopLyrics ? '关闭桌面歌词' : '桌面歌词'">
          <el-icon :size="18"><Reading /></el-icon>
        </div>
        <div class="visualizer-toggle" @click="showVisualizer = !showVisualizer" :class="{ active: showVisualizer }" title="频谱可视化">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M3 13h2v7H3v-7zm4-3h2v10H7V10zm4 4h2v6h-2v-6zm4-2h2v8h-2v-8zm4 6h2v2h-2v-2z"/></svg>
        </div>
        <div class="equalizer-btn" @click="openEqualizer" :class="{ active: activePreset !== 'normal' }" title="音效设置">
          <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M10 20h4V4h-4v16zm-6 0h4v-8H4v8zM16 9v11h4V9h-4z"/></svg>
        </div>
        <div class="sleep-timer-btn" @click="showSleepTimer = true" :class="{ active: sleepTimerMinutes > 0 }">
          <el-icon :size="18"><Clock /></el-icon>
          <span v-if="sleepTimerMinutes > 0" class="timer-badge">{{ sleepTimerMinutes }}</span>
        </div>
        <div class="volume-control">
          <el-icon @click="toggleMute"><component :is="volumeIcon" /></el-icon>
          <el-slider v-model="volume" :show-tooltip="false" style="width: 80px" @change="handleVolumeChange" />
        </div>
      </div>
    </div>

    <!-- 断点续播：切到有进度的歌时提示 -->
    <div v-if="pendingResume" class="resume-prompt">
      <span class="resume-text">这首歌上次听到 <b>{{ fmtSec(pendingResume.position) }}</b></span>
      <button class="resume-act primary" @click="confirmResume">继续播放</button>
      <button class="resume-act" @click="dismissResume">从头开始</button>
    </div>

    <!-- 打开网站时的续播入口 -->
    <div v-if="resumeInfo && !currentSong" class="resume-chip" @click="playResume">
      <span class="chip-label">继续上次</span>
      <span class="chip-song">{{ resumeInfo.song.name }}</span>
      <span class="chip-pos">{{ fmtSec(resumeInfo.position) }}</span>
      <span class="chip-close" @click.stop="resumeInfo = null">×</span>
    </div>

    <audio
      ref="audioRef"
      :src="normalizeUrl(currentSong?.url)"
      @play="onPlay"
      @pause="onPause"
      @ended="onEnded"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
      @error="onError"
    ></audio>

    <Teleport to="body">
    <div v-if="showLyrics && lyricContent" class="lyrics-panel" @click="showLyrics = false">
      <div class="lyrics-overlay"></div>
      <div class="lyrics-content" @click.stop>
        <div class="lyrics-header">
          <h3>{{ currentSong.name }}</h3>
          <p>{{ currentSong.singer }}</p>
          <el-button circle size="small" @click="showLyrics = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="lyrics-toolbar">
          <div class="lyric-offset-control">
            <span class="offset-label">歌词偏移</span>
            <el-button size="small" text @click="adjustOffset(-0.5)" :disabled="lyricOffset <= -5">-</el-button>
            <span class="offset-value">{{ lyricOffset > 0 ? '+' : '' }}{{ lyricOffset.toFixed(1) }}s</span>
            <el-button size="small" text @click="adjustOffset(0.5)" :disabled="lyricOffset >= 5">+</el-button>
            <el-button size="small" text v-if="lyricOffset !== 0" @click="adjustOffset(-lyricOffset)">重置</el-button>
          </div>
        </div>
        <div class="lyrics-scroll" ref="lyricsScrollRef">
          <div
            v-for="(line, index) in parsedLyrics"
            :key="index"
            class="lyric-line"
            :class="{
              active: currentLyricIndex === index,
              nearby: Math.abs(currentLyricIndex - index) === 1,
              dim: currentLyricIndex >= 0 && Math.abs(currentLyricIndex - index) > 1
            }"
            @click="seekToLyricWithAudio(line.time)"
          >
            <span class="lyric-text">{{ line.text }}</span>
            <span class="lyric-translation" v-if="line.translation">{{ line.translation }}</span>
          </div>
          <div v-if="!parsedLyrics.length" class="lyric-empty">暂无歌词</div>
        </div>
      </div>
    </div>
    </Teleport>

    <el-dialog v-model="showSleepTimer" title="定时关闭" width="300px" :append-to-body="true" class="sleep-timer-dialog">
      <div class="timer-options">
        <div
          v-for="opt in timerOptions"
          :key="opt.value"
          class="timer-option"
          :class="{ selected: sleepTimerMinutes === opt.value }"
          @click="setSleepTimer(opt.value)"
        >
          <span>{{ opt.label }}</span>
        </div>
        <div
          class="timer-option cancel"
          :class="{ selected: sleepTimerMinutes === 0 }"
          @click="cancelSleepTimer"
        >
          <span>关闭定时</span>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="showEqualizer" title="音效设置" width="360px" :append-to-body="true" class="equalizer-dialog">
      <div class="eq-presets">
        <div
          v-for="preset in eqPresets"
          :key="preset.key"
          :class="['eq-preset-card', { active: activePreset === preset.key }]"
          @click="applyPreset(preset.key)"
        >
          <div class="eq-preset-icon">{{ preset.icon }}</div>
          <div class="eq-preset-name">{{ preset.name }}</div>
        </div>
      </div>
      <div class="eq-bars" v-if="activePreset !== 'normal'">
        <div class="eq-bar-item" v-for="(bar, idx) in eqBars" :key="idx">
          <el-slider v-model="bar.gain" :min="-12" :max="12" :step="1" vertical height="100px" @change="updateEQ" />
          <span class="eq-bar-label">{{ bar.label }}</span>
        </div>
      </div>
      <div class="eq-tip" v-if="activePreset === 'normal'">选择预设音效，或自定义均衡器参数</div>
    </el-dialog>

    <Teleport to="body">
      <div v-if="showSpeedMenu" class="speed-overlay" @click="showSpeedMenu = false">
        <div class="speed-menu" @click.stop>
          <div
            v-for="rate in speedOptions"
            :key="rate"
            :class="['speed-item', { active: playbackRate === rate }]"
            @click="setPlaybackRate(rate)"
          >{{ rate }}x</div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
    <div v-if="showDesktopLyrics" class="desktop-lyrics" :style="{ left: desktopLyricsPos.x + 'px', top: desktopLyricsPos.y + 'px' }">
      <div class="desktop-lyrics-header" @mousedown="startDrag">
        <div class="desktop-lyrics-song-title">{{ currentSong?.name || '' }}<span v-if="currentSong?.singer" class="desktop-lyrics-singer"> - {{ currentSong.singer }}</span></div>
        <div class="desktop-lyrics-actions" @mousedown.stop>
          <div class="desktop-lyrics-action" @click.stop="playPrev" title="上一首">
            <el-icon :size="14"><DArrowLeft /></el-icon>
          </div>
          <div class="desktop-lyrics-action" @click.stop="togglePlay" title="播放/暂停">
            <el-icon :size="14"><VideoPause v-if="isPlaying" /><VideoPlay v-else /></el-icon>
          </div>
          <div class="desktop-lyrics-action" @click.stop="playNext" title="下一首">
            <el-icon :size="14"><DArrowRight /></el-icon>
          </div>
          <div class="desktop-lyrics-volume-wrap">
            <el-icon :size="14" class="desktop-lyrics-volume-icon" @click.stop="toggleMute"><component :is="volumeIcon" /></el-icon>
            <el-slider v-model="volume" :show-tooltip="false" size="small" @change="handleVolumeChange" class="desktop-lyrics-volume-slider" />
          </div>
          <div class="desktop-lyrics-close" @click.stop="showDesktopLyrics = false" title="关闭">
            <el-icon :size="14"><Close /></el-icon>
          </div>
        </div>
      </div>
      <div class="desktop-lyrics-body">
        <template v-for="(line, idx) in desktopLyricLines">
          <div
            :key="idx"
            :class="['desktop-lyrics-line', line.type]"
            v-if="line.text"
          >
            <span class="dl-text">{{ line.text }}</span>
            <span class="dl-translation" v-if="line.translation">{{ line.translation }}</span>
          </div>
        </template>
        <div v-if="!parsedLyrics.length" class="lyric-empty">暂无歌词</div>
      </div>
    </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { useLyric } from '@/composables/useLyric'
import AudioVisualizer from './AudioVisualizer.vue'
import { recordPlay } from '@/api/playHistory'
import { saveProgress, getProgress, getResume } from '@/api/progress'
import { getSongLyric, downloadSong, updateSongDuration } from '@/api/song'
import { ElMessage } from 'element-plus'

const store = useMusicStore()
const userStore = useUserStore()
const audioRef = ref(null)

// ==================== 断点续播 / 跨设备续听 ====================
// pendingResume：切到某首歌时，服务端发现这首歌有上次的进度，提示"继续播放"
// resumeInfo   ：打开网站时，服务端有"最近没听完的歌"，显示一个续播入口
const pendingResume = ref(null)
const resumeInfo = ref(null)
let pendingSeek = 0             // 待跳转的位置（等元数据加载完再跳）
let lastProgressAt = 0          // 上次上报的时间戳，用于节流

const fmtSec = (s) => {
  const v = Math.max(0, Math.floor(Number(s) || 0))
  return `${Math.floor(v / 60)}:${String(v % 60).padStart(2, '0')}`
}
const volume = ref(store.volume)
const isMuted = ref(false)
const isLiked = ref(false)
const showLyrics = ref(false)
const lyricsScrollRef = ref(null)
const lyricContent = ref('')
const {
  parsedLyrics,
  currentLyricIndex,
  currentLyricText,
  seekToLine: seekToLyric,
  offset: lyricOffset,
  adjustOffset
} = useLyric(lyricContent)
const showSleepTimer = ref(false)
const sleepTimerMinutes = ref(0)
let sleepTimerId = null
const showEqualizer = ref(false)
const activePreset = ref(localStorage.getItem('eq-preset') || 'normal')
let audioContext = null
let sourceNode = null
let eqFilters = []
let eqInitialized = false
const analyserNode = ref(null)
const showVisualizer = ref(false)
const visualizerWidth = ref(280)

const playbackRate = ref(parseFloat(localStorage.getItem('playback-rate') || '1'))
const showSpeedMenu = ref(false)
const speedOptions = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0]

function setPlaybackRate(rate) {
  playbackRate.value = rate
  localStorage.setItem('playback-rate', rate)
  if (audioRef.value) {
    audioRef.value.playbackRate = rate
  }
  showSpeedMenu.value = false
}

function destroyAudioContext() {
  if (sourceNode) {
    try { sourceNode.disconnect() } catch (e) { /* ignore */ }
    sourceNode = null
  }
  eqFilters.forEach(f => {
    try { f.disconnect() } catch (e) { /* ignore */ }
  })
  eqFilters = []
  if (analyserNode.value) {
    try { analyserNode.value.disconnect() } catch (e) { /* ignore */ }
    analyserNode.value = null
  }
  if (audioContext) {
    audioContext.close().catch(() => {})
    audioContext = null
  }
  eqInitialized = false
}

const eqPresets = [
  { key: 'normal', name: '标准', icon: '🎵', gains: [0, 0, 0, 0, 0] },
  { key: 'pop', name: '流行', icon: '🎤', gains: [1, 3, 5, 3, 1] },
  { key: 'rock', name: '摇滚', icon: '🎸', gains: [4, 2, -1, 2, 4] },
  { key: 'classical', name: '古典', icon: '🎻', gains: [3, 2, 0, 2, 3] },
  { key: 'electronic', name: '电子', icon: '🎹', gains: [4, 2, -2, 2, 5] },
  { key: 'vocal', name: '人声', icon: '🗣️', gains: [-2, 0, 4, 3, 0] },
  { key: 'bass', name: '重低音', icon: '🔊', gains: [6, 4, 0, -1, -2] },
]

const eqBars = ref([
  { label: '60Hz', gain: 0 },
  { label: '230Hz', gain: 0 },
  { label: '910Hz', gain: 0 },
  { label: '4kHz', gain: 0 },
  { label: '14kHz', gain: 0 },
])

const initAudioContext = () => {
  if (eqInitialized) return
  if (!audioRef.value) return

  destroyAudioContext()

  try {
    audioContext = new (window.AudioContext || window.webkitAudioContext)()
    if (audioContext.state === 'suspended') {
      audioContext.resume()
    }
    sourceNode = audioContext.createMediaElementSource(audioRef.value)

    analyserNode.value = audioContext.createAnalyser()
    analyserNode.value.fftSize = 256

    const frequencies = [60, 230, 910, 4000, 14000]
    eqFilters = frequencies.map((freq, i) => {
      const filter = audioContext.createBiquadFilter()
      filter.type = i === 0 ? 'lowshelf' : i === frequencies.length - 1 ? 'highshelf' : 'peaking'
      filter.frequency.value = freq
      filter.Q.value = 1
      filter.gain.value = 0
      return filter
    })

    sourceNode.connect(analyserNode.value)
    analyserNode.value.connect(eqFilters[0])
    for (let i = 0; i < eqFilters.length - 1; i++) {
      eqFilters[i].connect(eqFilters[i + 1])
    }
    eqFilters[eqFilters.length - 1].connect(audioContext.destination)

    const saved = eqPresets.find(p => p.key === activePreset.value)
    if (saved) {
      saved.gains.forEach((g, i) => {
        eqFilters[i].gain.value = g
        eqBars.value[i].gain = g
      })
    }

    eqInitialized = true
  } catch (e) {
    console.error('AudioContext init failed:', e)
    destroyAudioContext()
  }
}

const openEqualizer = () => {
  showEqualizer.value = true
  initAudioContext()
}

const applyPreset = (key) => {
  activePreset.value = key
  localStorage.setItem('eq-preset', key)
  const preset = eqPresets.find(p => p.key === key)
  if (preset) {
    preset.gains.forEach((g, i) => {
      eqBars.value[i].gain = g
      if (eqFilters[i]) eqFilters[i].gain.value = g
    })
  }
}

const updateEQ = () => {
  eqBars.value.forEach((bar, i) => {
    if (eqFilters[i]) eqFilters[i].gain.value = bar.gain
  })
}

const showDesktopLyrics = ref(false)
const desktopLyricsPos = ref({ x: window.innerWidth - 420, y: 100 })
const desktopLyricsScrollRef = ref(null)
let isDragging = false
let dragOffset = { x: 0, y: 0 }

const desktopLyricLines = computed(() => {
  const idx = currentLyricIndex.value
  const lines = []
  if (idx < 0) return lines
  if (idx > 0) {
    lines.push({ type: 'prev', text: parsedLyrics.value[idx - 1]?.text || '', translation: parsedLyrics.value[idx - 1]?.translation || '' })
  }
  lines.push({ type: 'current', text: parsedLyrics.value[idx]?.text || '', translation: parsedLyrics.value[idx]?.translation || '' })
  if (idx + 1 < parsedLyrics.value.length) {
    lines.push({ type: 'next', text: parsedLyrics.value[idx + 1]?.text || '', translation: parsedLyrics.value[idx + 1]?.translation || '' })
  }
  if (idx + 2 < parsedLyrics.value.length) {
    lines.push({ type: 'far', text: parsedLyrics.value[idx + 2]?.text || '', translation: '' })
  }
  return lines
})

const normalizeUrl = (url) => {
  if (!url) return url
  if (url.includes('localhost:8080') || url.startsWith('/uploads/') || url.startsWith('/api/')) {
    return url.replace(/^https?:\/\/[^/]+/, '')
  }
  return url
}

const timerOptions = [
  { label: '15分钟后', value: 15 },
  { label: '30分钟后', value: 30 },
  { label: '60分钟后', value: 60 },
  { label: '90分钟后', value: 90 },
]

const currentSong = ref(store.currentSong)
const isPlaying = ref(store.isPlaying)
const currentTime = computed(() => store.currentTime)
const duration = computed(() => store.duration)

const progressPercent = computed(() => {
  if (store.duration === 0) return 0
  return (store.currentTime / store.duration) * 100
})

const modeSvg = computed(() => {
  const svgs = {
    list: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>',
    random: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/><line x1="4" y1="4" x2="9" y2="9"/></svg>',
    single: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/><text x="12" y="16" text-anchor="middle" font-size="7" fill="currentColor" stroke="none" font-weight="bold">1</text></svg>'
  }
  return svgs[store.playMode] || svgs.list
})

const modeText = computed(() => {
  const texts = { list: '列表循环', random: '随机播放', single: '单曲循环' }
  return texts[store.playMode] || '列表循环'
})

const volumeIcon = computed(() => {
  if (isMuted.value || volume.value === 0) return 'Mute'
  if (volume.value < 50) return 'TurnOff'
  return 'Volume'
})

let songChanging = false

watch(() => store.currentSong, async (newSong) => {
  songChanging = true
  currentSong.value = newSong
  lyricContent.value = ''
  if (newSong?.url && audioRef.value) {
    await nextTick()
    if (eqInitialized) {
      destroyAudioContext()
      initAudioContext()
    }
    audioRef.value.playbackRate = playbackRate.value
    audioRef.value.load()
    try {
      await audioRef.value.play()
      if (userStore.userInfo?.userId && newSong?.id) {
        recordPlay(newSong.id).catch(() => {})
        // 检查这首歌有没有上次的进度，有就弹出"继续播放"
        checkResumePosition(newSong)
        lastProgressAt = 0
        pendingResume.value = null
      }
    } catch (e) {
      console.error('播放失败:', e)
    }
  }
  if (newSong?.id && newSong?.lyric) {
    try {
      const res = await getSongLyric(newSong.id)
      lyricContent.value = res.data || ''
    } catch (e) {
      lyricContent.value = ''
    }
  }
  songChanging = false
})

watch(() => currentLyricIndex.value, (newIndex) => {
  if (newIndex >= 0 && lyricsScrollRef.value) {
    const lines = lyricsScrollRef.value.querySelectorAll('.lyric-line')
    if (lines[newIndex]) {
      lines[newIndex].scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
  if (newIndex >= 0 && desktopLyricsScrollRef.value) {
    const lines = desktopLyricsScrollRef.value.querySelectorAll('.desktop-lyric-line')
    if (lines[newIndex]) {
      lines[newIndex].scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
})

watch(() => store.isPlaying, (val) => {
  isPlaying.value = val
  if (songChanging) return
  if (val) {
    audioRef.value?.play().catch(() => {})
  } else {
    audioRef.value?.pause()
  }
})

watch(() => store.playMode, () => {})

onUnmounted(() => {
  if (sleepTimerId) {
    clearTimeout(sleepTimerId)
  }
  destroyAudioContext()
})

const togglePlay = () => {
  store.togglePlay()
}

const currentList = computed(() => store.getCurrentList())

const playPrev = () => {
  if (currentList.value.length === 0) return
  const currentIndex = currentList.value.findIndex(s => s.id === currentSong.value?.id)
  if (store.isQueueMode) {
    if (currentIndex > 0) {
      store.setCurrentSong(currentList.value[currentIndex - 1])
    }
  } else {
    if (store.playMode === 'random') {
      const randomIndex = Math.floor(Math.random() * currentList.value.length)
      store.setCurrentSong(currentList.value[randomIndex])
    } else if (currentIndex > 0) {
      store.setCurrentSong(currentList.value[currentIndex - 1])
    } else {
      store.setCurrentSong(currentList.value[currentList.value.length - 1])
    }
  }
}

const playNext = () => {
  if (currentList.value.length === 0) return
  const currentIndex = currentList.value.findIndex(s => s.id === currentSong.value?.id)
  if (store.isQueueMode) {
    if (currentIndex < currentList.value.length - 1) {
      store.setCurrentSong(currentList.value[currentIndex + 1])
    }
  } else {
    if (store.playMode === 'random') {
      const randomIndex = Math.floor(Math.random() * currentList.value.length)
      store.setCurrentSong(currentList.value[randomIndex])
    } else if (currentIndex < currentList.value.length - 1) {
      store.setCurrentSong(currentList.value[currentIndex + 1])
    } else {
      store.setCurrentSong(currentList.value[0])
    }
  }
}

const onEnded = () => {
  // 播完了：清掉这首歌的进度，下次从头开始
  if (currentSong.value?.id && userStore.userInfo?.userId) {
    saveProgress(currentSong.value.id, 0).catch(() => {})
  }
  if (store.isQueueMode) {
    const currentIndex = currentList.value.findIndex(s => s.id === currentSong.value?.id)
    if (currentIndex < currentList.value.length - 1) {
      playNext()
    }
  } else if (store.playMode === 'single') {
    audioRef.value.currentTime = 0
    audioRef.value.play()
  } else {
    playNext()
  }
}

const onTimeUpdate = () => {
  if (audioRef.value) {
    store.setCurrentTime(audioRef.value.currentTime)
    // 播放中每 10 秒上报一次进度（节流由 reportProgress 内部判断）
    if (store.isPlaying) reportProgress()
  }
}

/** 上报播放位置（节流 10 秒；force=true 立即上报，用于暂停/切歌） */
const reportProgress = (force = false) => {
  const song = currentSong.value
  if (!song?.id || !userStore.userInfo?.userId) return
  const now = Date.now()
  if (!force && now - lastProgressAt < 10000) return
  lastProgressAt = now
  const pos = Math.floor(audioRef.value?.currentTime || 0)
  saveProgress(song.id, pos).catch(() => {})
}

/** 切歌后检查这首歌有没有上次的进度 */
const checkResumePosition = async (song) => {
  pendingResume.value = null
  if (!song?.id || !userStore.userInfo?.userId) return
  try {
    const res = await getProgress(song.id)
    const pos = res.data?.position || 0
    if (pos < 5) return
    const dur = song.duration || 0
    // 已经接近结尾的就不提示了
    if (dur && pos > dur - 10) return
    pendingResume.value = { songId: song.id, position: pos }
  } catch (e) { /* 静默失败，不影响播放 */ }
}

const confirmResume = async () => {
  if (!pendingResume.value || !audioRef.value) return
  const pos = pendingResume.value.position
  pendingResume.value = null

  if (!audioRef.value.duration) {
    // 页面刚打开、音频还没加载元数据：先播放，等 loadedmetadata 再跳（见 onLoadedMetadata）
    pendingSeek = pos
    await audioRef.value.play().catch(() => {})
    store.isPlaying = true
  } else {
    audioRef.value.currentTime = pos
    store.setCurrentTime(pos)
    audioRef.value.play().catch(() => {})
  }
  ElMessage.success(`已跳到上次听到的 ${fmtSec(pos)}`)
}

const dismissResume = () => {
  if (pendingResume.value && userStore.userInfo?.userId) {
    // 选择从头听 → 清掉服务端进度
    saveProgress(pendingResume.value.songId, 0).catch(() => {})
  }
  pendingResume.value = null
}

/** 打开网站时拉一次续播信息 */
const loadResumeOnStart = async () => {
  if (!userStore.userInfo?.userId) return
  try {
    if (store.currentSong) {
      // 场景一：关标签页前正在听某首歌（localStorage 里存着）→ 直接检查这首歌的进度
      await checkResumePosition(store.currentSong)
    } else {
      // 场景二：本机没有播放记录（比如换了设备/浏览器）→ 问服务端"最近听的最后一首"
      // 此时播放器里没有歌，用右下角的「继续上次」小条，点击后才加载并跳转
      const res = await getResume()
      if (res.data?.song) {
        resumeInfo.value = { song: res.data.song, position: res.data.position || 0 }
      }
    }
  } catch (e) { /* 静默失败，不影响播放 */ }
}

/** 点击「继续上次播放」 */
const playResume = () => {
  const info = resumeInfo.value
  if (!info) return
  resumeInfo.value = null
  pendingSeek = info.position || 0
  store.setPlayList([info.song])
  store.setCurrentSong(info.song)
}

const onLoadedMetadata = () => {
  if (audioRef.value) {
    store.setDuration(audioRef.value.duration)
    // 元数据就绪后再跳转，否则 currentTime 赋值无效
    if (pendingSeek > 0) {
      audioRef.value.currentTime = pendingSeek
      store.setCurrentTime(pendingSeek)
      pendingSeek = 0
    }
    if (currentSong.value?.id && (!currentSong.value.duration || currentSong.value.duration === 0)) {
      const dur = Math.round(audioRef.value.duration)
      if (dur > 0) {
        currentSong.value.duration = dur
        updateSongDuration(currentSong.value.id, dur).catch(() => {})
      }
    }
  }
}

const onError = () => {
  console.error('音频加载失败')
}

const handleDownload = () => {
  if (!store.currentSong?.id) return
  downloadSong(store.currentSong.id)
}

const setSleepTimer = (minutes) => {
  if (sleepTimerId) {
    clearTimeout(sleepTimerId)
  }
  sleepTimerMinutes.value = minutes
  showSleepTimer.value = false
  sleepTimerId = setTimeout(() => {
    if (audioRef.value && !audioRef.value.paused) {
      audioRef.value.pause()
    }
    sleepTimerMinutes.value = 0
  }, minutes * 60 * 1000)
}

const cancelSleepTimer = () => {
  if (sleepTimerId) {
    clearTimeout(sleepTimerId)
    sleepTimerId = null
  }
  sleepTimerMinutes.value = 0
  showSleepTimer.value = false
}

const onPlay = () => {
  store.isPlaying = true
}
const onPause = () => {
  store.isPlaying = false
  reportProgress(true)   // 暂停时立刻存一次，避免丢最后一段进度
}

onMounted(() => {
  // 打开网站时，如果服务端有「上次没听完」的歌，给一个续播入口
  loadResumeOnStart()
})

const handleProgressClick = (e) => {  if (!audioRef.value || store.duration === 0) return
  const rect = e.currentTarget.getBoundingClientRect()
  const percent = (e.clientX - rect.left) / rect.width
  const time = percent * store.duration
  audioRef.value.currentTime = time
  store.seekTo(time)
}

const handleVolumeChange = (val) => {
  if (audioRef.value) {
    audioRef.value.volume = val / 100
  }
  store.setVolume(val)
  isMuted.value = val === 0
}

const toggleMute = () => {
  isMuted.value = !isMuted.value
  if (audioRef.value) {
    audioRef.value.muted = isMuted.value
  }
}

const togglePlayMode = () => {
  const modes = ['list', 'random', 'single']
  const currentIndex = modes.indexOf(store.playMode)
  store.setPlayMode(modes[(currentIndex + 1) % modes.length])
}

const toggleLike = () => {
  isLiked.value = !isLiked.value
}

const seekToLyricWithAudio = (time) => {
  if (!audioRef.value) return
  audioRef.value.currentTime = time
  seekToLyric(time)
}

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

const toggleDesktopLyrics = () => {
  showDesktopLyrics.value = !showDesktopLyrics.value
  store.showDesktopLyrics = showDesktopLyrics.value
}

watch(() => store.showDesktopLyrics, (val) => {
  showDesktopLyrics.value = val
})

watch(() => store.seekTarget, (time) => {
  if (time != null && !isNaN(time) && audioRef.value) {
    audioRef.value.currentTime = time
  }
})

const startDrag = (e) => {
  if (e.target.closest('.desktop-lyrics-actions')) return
  isDragging = true
  dragOffset.x = e.clientX - desktopLyricsPos.value.x
  dragOffset.y = e.clientY - desktopLyricsPos.value.y
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (!isDragging) return
  desktopLyricsPos.value.x = e.clientX - dragOffset.x
  desktopLyricsPos.value.y = e.clientY - dragOffset.y
}

const stopDrag = () => {
  isDragging = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}
</script>

<style scoped>
.player {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  z-index: 100;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}

.progress-bar {
  height: 3px;
  cursor: pointer;
  position: relative;
  transition: height 0.2s;
}

.progress-bar:hover {
  height: 5px;
}

.progress-bg {
  height: 100%;
  background: var(--bg-elevated);
  position: relative;
}

.progress-fill {
  height: 100%;
  background: var(--gradient-neon);
  position: relative;
  transition: width 0.1s linear;
  box-shadow: 0 0 8px rgba(34, 211, 238, 0.3);
}

.progress-dot {
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  background: var(--accent-cyan);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
  box-shadow: 0 0 8px rgba(34, 211, 238, 0.5);
}

.progress-bar:hover .progress-dot {
  opacity: 1;
}

.player-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.song-info {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 280px;
}

.cover-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
}

.cover-wrapper.playing .cover,
.cover-wrapper.playing .cover-placeholder {
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.song-detail {
  overflow: hidden;
  flex: 1;
}

.song-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-singer {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-lyric {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.3s;
}

.like-btn {
  color: var(--text-tertiary);
}

.download-btn {
  color: var(--text-tertiary);
}

.download-btn:hover {
  color: var(--primary-color);
}

.controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mode-btn {
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
}

.mode-btn:hover {
  color: var(--primary-color);
}

.mode-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

.mode-icon svg {
  width: 100%;
  height: 100%;
}

.controls :deep(.el-button) {
  color: var(--text-secondary);
  border-color: var(--border-color);
  background: transparent;
  border-radius: 50%;
}

.controls :deep(.el-button:hover) {
  color: var(--text-primary);
  border-color: var(--border-glow);
  background: var(--nav-hover-bg);
}

.controls :deep(.el-button.is-primary) {
  color: #fff;
  border-color: transparent;
  background: var(--gradient-primary);
  box-shadow: var(--neon-glow-sm);
}

.controls :deep(.el-button.is-primary:hover) {
  box-shadow: var(--neon-glow);
}

.controls :deep(.el-button.is-disabled) {
  opacity: 0.3;
}

.right-controls {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 280px;
  justify-content: flex-end;
}

.time-display {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.sleep-timer-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
  position: relative;
}

.equalizer-btn {
  display: flex;
  align-items: center;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.equalizer-btn:hover {
  color: var(--text-primary);
  background: var(--nav-hover-bg);
}

.equalizer-btn.active {
  color: var(--primary-color);
}

.eq-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.eq-preset-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  border-radius: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.eq-preset-card:hover {
  border-color: var(--border-glow);
  background: var(--nav-hover-bg);
}

.eq-preset-card.active {
  border-color: var(--primary-color);
  background: var(--nav-active-bg);
  box-shadow: var(--neon-glow-sm);
}

.eq-preset-icon {
  font-size: 22px;
}

.eq-preset-name {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.eq-preset-card.active .eq-preset-name {
  color: var(--primary-color);
}

.eq-bars {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 16px 0;
}

.eq-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.eq-bar-label {
  font-size: 11px;
  color: var(--text-tertiary);
}

.eq-tip {
  text-align: center;
  color: var(--text-tertiary);
  font-size: 13px;
  padding: 20px 0;
}

.sleep-timer-btn:hover {
  color: var(--text-primary);
  background: var(--nav-hover-bg);
}

.sleep-timer-btn.active {
  color: var(--primary-color);
}

.timer-badge {
  font-size: 10px;
  background: var(--gradient-primary);
  color: #fff;
  padding: 1px 4px;
  border-radius: 8px;
  line-height: 1;
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
}

.volume-control :deep(.el-slider__runway) {
  background: var(--bg-elevated);
}

.volume-control :deep(.el-slider__bar) {
  background: var(--gradient-primary);
}

.volume-control :deep(.el-slider__button) {
  border-color: var(--primary-color);
  box-shadow: var(--neon-glow-sm);
}

.lyrics-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 76px;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lyrics-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(7, 7, 14, 0.9);
  backdrop-filter: blur(30px);
}

.lyrics-content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 600px;
  height: 80%;
  background: var(--glass-bg);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--neon-glow);
}

.lyrics-header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid var(--border-color);
  position: relative;
}

.lyrics-header h3 {
  color: var(--text-primary);
  font-size: 20px;
  margin: 0 0 8px;
}

.lyrics-header p {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.lyrics-header .el-button {
  position: absolute;
  top: 16px;
  right: 16px;
  color: var(--text-secondary);
  border-color: var(--border-color);
  background: transparent;
}

.lyrics-header .el-button:hover {
  color: var(--text-primary);
  border-color: var(--border-glow);
}

.lyrics-toolbar {
  padding: 10px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: center;
}

.lyric-offset-control {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.offset-label {
  color: var(--text-tertiary);
}

.offset-value {
  min-width: 48px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}

.lyrics-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 40px 20px;
  scroll-behavior: smooth;
}

.lyrics-scroll::-webkit-scrollbar {
  width: 4px;
}

.lyrics-scroll::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 2px;
}

.lyric-line {
  padding: 12px 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 16px;
  line-height: 1.8;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  transform: scale(0.92);
  opacity: 0.4;
}

.lyric-line:hover {
  color: var(--text-secondary);
}

.lyric-line.nearby {
  opacity: 0.7;
  transform: scale(0.96);
}

.lyric-line.dim {
  opacity: 0.3;
  transform: scale(0.88);
}

.lyric-line.active {
  color: var(--primary-color);
  font-size: 18px;
  font-weight: 500;
  text-shadow: 0 0 20px rgba(99, 102, 241, 0.3);
  transform: scale(1);
  opacity: 1;
}

.lyric-text {
  display: block;
}

.lyric-translation {
  display: block;
  font-size: 13px;
  color: var(--text-tertiary);
  margin-top: 2px;
  font-weight: 400;
}

.lyric-line.active .lyric-translation {
  color: rgba(99, 102, 241, 0.6);
}

.lyric-empty {
  text-align: center;
  color: var(--text-tertiary);
  padding: 40px 0;
}

.timer-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.timer-option {
  padding: 12px 16px;
  border-radius: 10px;
  background: var(--bg-elevated);
  cursor: pointer;
  text-align: center;
  font-size: 14px;
  color: var(--text-primary);
  transition: all 0.2s;
  border: 1px solid var(--border-color);
}

.timer-option:hover {
  border-color: var(--border-glow);
  background: var(--nav-hover-bg);
}

.timer-option.selected {
  background: var(--gradient-primary);
  color: #fff;
  border-color: transparent;
  box-shadow: var(--neon-glow-sm);
}

.timer-option.cancel {
  margin-top: 8px;
  background: rgba(244, 114, 182, 0.08);
  color: var(--accent-pink);
  border-color: rgba(244, 114, 182, 0.15);
}

.timer-option.cancel:hover {
  background: rgba(244, 114, 182, 0.15);
}

.timer-option.cancel.selected {
  background: var(--accent-pink);
  color: #fff;
  border-color: transparent;
}

:deep(.sleep-timer-dialog .el-dialog__header) {
  margin-bottom: 16px;
}

:deep(.sleep-timer-dialog .el-dialog__title) {
  font-weight: 600;
}

.speed-control {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  background: var(--bg-elevated);
  transition: all 0.2s;
  user-select: none;
}

.speed-control:hover {
  color: var(--text-primary);
  background: var(--nav-hover-bg);
}

.speed-control.active {
  color: var(--primary-color);
}

.speed-overlay {
  position: fixed;
  inset: 0;
  z-index: 20000;
}

.speed-menu {
  position: fixed;
  bottom: 100px;
  right: 200px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 6px;
  box-shadow: var(--card-shadow);
  display: flex;
  gap: 4px;
}

.speed-item {
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.speed-item:hover {
  background: var(--nav-hover-bg);
  color: var(--text-primary);
}

.speed-item.active {
  background: var(--primary-light);
  color: var(--primary-color);
}

.visualizer-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--text-tertiary);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.visualizer-toggle:hover {
  color: var(--text-primary);
  background: var(--nav-hover-bg);
}

.visualizer-toggle.active {
  color: var(--primary-color);
}

.visualizer-bar {
  padding: 4px 16px 0;
  display: flex;
  justify-content: center;
}

.desktop-lyrics-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--text-tertiary);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.desktop-lyrics-btn:hover {
  color: var(--text-primary);
  background: var(--nav-hover-bg);
}

.desktop-lyrics-btn.active {
  color: var(--primary-color);
}

.desktop-lyrics {
  position: fixed;
  z-index: 9999;
  width: 640px;
  background: rgba(10, 10, 30, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(99, 102, 241, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), 0 0 60px rgba(99, 102, 241, 0.08);
}

.desktop-lyrics-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: move;
  flex-shrink: 0;
  gap: 12px;
  min-height: 40px;
  user-select: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.desktop-lyrics-song-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: 0 0 8px rgba(99, 102, 241, 0.4);
  flex-shrink: 1;
  min-width: 0;
}

.desktop-lyrics-singer {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

.desktop-lyrics-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.desktop-lyrics-action {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.desktop-lyrics-action:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.desktop-lyrics-volume-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.desktop-lyrics-volume-wrap:hover {
  background: rgba(255, 255, 255, 0.08);
}

.desktop-lyrics-volume-icon {
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  flex-shrink: 0;
}

.desktop-lyrics-volume-icon:hover {
  color: #fff;
}

.desktop-lyrics-volume-slider {
  width: 60px;
  --el-slider-height: 3px;
  --el-slider-button-size: 10px;
  --el-slider-main-bg-color: rgba(99, 102, 241, 0.8);
  --el-slider-runway-bg-color: rgba(255, 255, 255, 0.15);
}

.desktop-lyrics-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  margin-left: 4px;
}

.desktop-lyrics-close:hover {
  color: #fff;
  background: rgba(255, 80, 80, 0.3);
}

.desktop-lyrics-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 24px 16px;
  overflow: hidden;
}

.desktop-lyrics-line {
  text-align: center;
  color: rgba(200, 220, 255, 0.9);
  font-size: 22px;
  font-weight: 400;
  line-height: 1.6;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  text-shadow: 0 0 10px rgba(99, 102, 241, 0.6), 0 0 20px rgba(99, 102, 241, 0.3);
}

.desktop-lyrics-line .dl-text {
  display: block;
}

.desktop-lyrics-line .dl-translation {
  display: block;
  font-size: 13px;
  color: rgba(180, 200, 240, 0.5);
  margin-top: 2px;
}

.desktop-lyrics-line.current {
  color: rgba(220, 230, 255, 1);
  font-size: 24px;
  font-weight: 500;
  text-shadow: 0 0 12px rgba(99, 102, 241, 0.8), 0 0 24px rgba(139, 92, 246, 0.4);
}

.desktop-lyrics-line.current .dl-translation {
  color: rgba(99, 102, 241, 0.6);
  font-size: 14px;
}

.desktop-lyrics-line.prev {
  font-size: 15px;
  font-weight: 400;
  color: rgba(180, 200, 240, 0.4);
  margin-bottom: 4px;
  text-shadow: 0 0 6px rgba(99, 102, 241, 0.2);
}

.desktop-lyrics-line.next {
  font-size: 16px;
  font-weight: 400;
  color: rgba(180, 200, 240, 0.6);
  margin-top: 6px;
  text-shadow: 0 0 8px rgba(99, 102, 241, 0.3);
}

.desktop-lyrics-line.far {
  font-size: 14px;
  font-weight: 400;
  color: rgba(180, 200, 240, 0.3);
  margin-top: 2px;
  text-shadow: none;
}

/* ==================== 断点续播 UI ==================== */
.resume-prompt {
  position: fixed;
  right: 24px;
  bottom: 96px;
  z-index: 3000;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(30, 32, 52, 0.96);
  border: 1px solid rgba(102, 126, 234, 0.35);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(8px);
  animation: resume-in 0.22s ease-out;
}
.resume-text b { color: #a8b4ff; font-weight: 600; }
.resume-act {
  padding: 4px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.65);
  transition: all 0.2s;
}
.resume-act:hover { background: rgba(255, 255, 255, 0.16); }
.resume-act.primary { background: rgba(102, 126, 234, 0.35); color: #c3ccff; }
.resume-act.primary:hover { background: rgba(102, 126, 234, 0.55); }

.resume-chip {
  position: fixed;
  right: 24px;
  bottom: 96px;
  z-index: 3000;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 20px;
  background: rgba(30, 32, 52, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  backdrop-filter: blur(8px);
  animation: resume-in 0.22s ease-out;
  transition: all 0.2s;
}
.resume-chip:hover { border-color: rgba(102, 126, 234, 0.5); background: rgba(40, 44, 72, 0.96); }
.chip-label { color: #a8b4ff; }
.chip-song { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: rgba(255,255,255,0.85); }
.chip-pos { color: rgba(255, 255, 255, 0.4); }
.chip-close { margin-left: 2px; color: rgba(255, 255, 255, 0.3); padding: 0 2px; }
.chip-close:hover { color: #f5576c; }

@keyframes resume-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

</style>
