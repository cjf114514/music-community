# 音乐网站全栈项目 — 功能学习指南

> 本文档面向学习者，逐一拆解本项目中 **8 个代表性功能** 的完整实现细节，涵盖前端 Vue 3 和后端 Spring Boot 两端代码。

---

## 目录

1. [项目技术栈概览](#1-项目技术栈概览)
2. [功能一：核心音乐播放器](#2-功能一核心音乐播放器)
3. [功能二：Web Audio API 音频可视化 + 均衡器](#3-功能二web-audio-api-音频可视化--均衡器)
4. [功能三：LRC 歌词解析与桌面歌词](#4-功能三lrc-歌词解析与桌面歌词)
5. [功能四：用户认证体系 (JWT + 验证码 + 邮箱)](#5-功能四用户认证体系-jwt--验证码--邮箱)
6. [功能五：多维搜索系统](#6-功能五多维搜索系统)
7. [功能六：音乐社区 (帖子 + 评论 + 点赞 + 关注)](#7-功能六音乐社区-帖子--评论--点赞--关注)
8. [功能七：管理员后台系统](#8-功能七管理员后台系统)
9. [功能八：Canvas 动态背景](#8-功能八canvas-动态背景)

---

## 1. 项目技术栈概览

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| **前端框架** | Vue 3 (Composition API) | 使用 `<script setup>` 语法糖 |
| **构建工具** | Vite | 快速冷启动 + HMR |
| **UI 组件库** | Element Plus | 对话框、表格、表单、滑块等 |
| **状态管理** | Pinia | Vue 3 官方推荐，替代 Vuex |
| **路由** | Vue Router 4 | HTML5 History 模式 + 路由守卫 |
| **HTTP 客户端** | Axios | 拦截器统一处理 Token 和 401 |
| **图表** | ECharts | 管理员后台数据可视化 |
| **后端框架** | Spring Boot 3.2.4 | Java 17+ 企业级框架 |
| **数据库** | MySQL + JPA (Hibernate) | ORM 自动建表 + 原生查询 |
| **认证** | JWT (JSON Web Token) | 无状态身份验证 |
| **密码加密** | Spring Security Crypto (BCrypt) | 不可逆哈希存储 |
| **文件存储** | 本地文件系统 | `/uploads/` 目录 |

### 项目目录结构

```
music/
├── frontend/src/
│   ├── api/            # API 接口封装 (每个功能模块一个文件)
│   ├── components/     # 全局可复用组件 (播放器、侧边栏、搜索框等)
│   ├── composables/    # 组合式函数 (歌词解析、音频分析)
│   ├── router/         # 路由配置 + 守卫
│   ├── stores/         # Pinia 状态管理 (music, user, theme, playlist)
│   └── views/          # 页面组件
│       └── admin/      # 管理员子页面
├── backend/src/main/java/com/music/
│   ├── controller/     # REST 控制器 (27 个)
│   ├── service/        # 业务逻辑层
│   ├── repository/     # JPA 数据访问层
│   ├── entity/         # 数据库实体
│   ├── dto/            # 数据传输对象
│   ├── config/         # Spring 配置 (拦截器注册、验证码)
│   ├── interceptor/    # 自定义拦截器 (JWT 验证、管理员权限)
│   └── annotation/     # 自定义注解 (@RequireAdmin)
└── database/           # SQL 建表脚本
```

---

## 2. 功能一：核心音乐播放器

### 2.1 功能概述

播放器是整个应用的"心脏"——它固定在页面底部，无论用户导航到哪个页面都不会卸载。支持以下子功能：

- 播放 / 暂停、上一首 / 下一首
- 三种播放模式：列表循环、单曲循环、随机播放
- 播放队列 (Queue) 模式 — 临时插队播放，不影响主播放列表
- 倍速播放 (0.5x ~ 2.0x)
- 定时关闭 (睡眠定时器)
- 歌曲下载
- 全局键盘快捷键
- 播放状态持久化 (刷新页面后恢复)

### 2.2 架构设计

播放器的状态由一个 **Pinia Store** (`music.js`) 集中管理，所有组件通过 Store 读写状态，而不是通过 props/emit 层层传递：

```
┌─────────────────────────────────────────────────────┐
│                   Pinia Store (music.js)             │
│  currentSong, isPlaying, playList, queue, volume...  │
│  playMode, currentTime, duration, showPlayList...    │
└──────┬──────────────┬──────────────┬────────────────┘
       │              │              │
  Player.vue    PlayListDrawer    App.vue (快捷键)
  (音频控制)     (播放列表UI)      (Space/方向键)
```

### 2.3 前端代码详解

#### 2.3.1 Pinia Store — 播放状态管理

文件：[stores/music.js](frontend/src/stores/music.js)

```javascript
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useMusicStore = defineStore('music', () => {
  // ==================== 状态持久化 ====================
  // 页面刷新时从 localStorage 恢复播放器状态
  // 这样用户刷新后不会丢失播放列表和当前歌曲
  const savedState = (() => {
    try {
      const raw = localStorage.getItem('music_player_state')
      return raw ? JSON.parse(raw) : null
    } catch { return null }
  })()

  const currentSong = ref(savedState?.currentSong || null)
  const isPlaying = ref(false)
  const playList = ref(savedState?.playList || [])
  const queue = ref(savedState?.queue || [])       // 临时队列
  const isQueueMode = ref(savedState?.isQueueMode || false)
  const volume = ref(savedState?.volume ?? 80)
  const currentTime = ref(0)
  const duration = ref(0)
  const playMode = ref(savedState?.playMode || 'list')  // 'list' | 'random' | 'single'

  // 监听状态变化 → 自动保存到 localStorage
  function saveState() {
    const state = {
      currentSong: currentSong.value,
      playList: playList.value,
      queue: queue.value,
      isQueueMode: isQueueMode.value,
      volume: volume.value,
      playMode: playMode.value
    }
    localStorage.setItem('music_player_state', JSON.stringify(state))
  }

  // watch 的第三个参数 { deep: true } 表示深度监听
  // 当播放列表中的歌曲信息变化时也会触发保存
  watch([currentSong, playList, queue, isQueueMode, volume, playMode], saveState, { deep: true })

  // ==================== 核心方法 ====================

  // 设置当前播放歌曲 — 同时重置播放进度
  function setCurrentSong(song) {
    currentSong.value = song
    isPlaying.value = true
    currentTime.value = 0
    duration.value = 0
  }

  // 获取当前有效的播放列表
  // 如果处于队列模式 → 返回 queue；否则返回主播放列表 playList
  function getCurrentList() {
    return isQueueMode.value ? queue.value : playList.value
  }

  // 播放下一首
  // 逻辑：在当前列表中找当前歌曲的位置 → 根据播放模式决定下一首
  function playNext() {
    const list = getCurrentList()
    if (list.length === 0) return
    const currentIdx = list.findIndex(s => s.id === currentSong.value?.id)
    let nextIdx
    if (playMode.value === 'random') {
      nextIdx = Math.floor(Math.random() * list.length)  // 随机
    } else if (currentIdx < list.length - 1) {
      nextIdx = currentIdx + 1  // 下一首
    } else {
      nextIdx = 0  // 到末尾则循环回第一首
    }
    setCurrentSong(list[nextIdx])
  }
  // ... (其余方法：playPrev, togglePlay, setVolume, seekTo 等)
})
```

**设计要点：**
- `localStorage` 持久化：刷新页面不会丢失播放状态
- 队列模式 (`queue`) 与主播放列表 (`playList`) 分离：用户可以临时插队听歌，不影响原有列表
- `watch` + `deep: true` 自动保存：任何状态变化都会自动写入 localStorage

#### 2.3.2 Player.vue — 播放器组件

文件：[components/Player.vue](frontend/src/components/Player.vue)

播放器组件是整个应用中最复杂的组件（~1600 行），这里拆解其核心机制：

**(1) `<audio>` 元素与状态同步**

```html
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
```

Vue 通过 `watch` 监听 Store 中 `currentSong` 的变化，当歌曲切换时自动加载新音频：

```javascript
watch(() => store.currentSong, async (newSong) => {
  currentSong.value = newSong
  lyricContent.value = ''   // 清空旧歌词
  if (newSong?.url && audioRef.value) {
    await nextTick()
    audioRef.value.playbackRate = playbackRate.value  // 应用倍速设置
    audioRef.value.load()    // 重新加载音频源
    try {
      await audioRef.value.play()   // 自动播放
      if (userStore.userInfo?.userId && newSong?.id) {
        recordPlay(newSong.id).catch(() => {})  // 记录播放历史
      }
    } catch (e) {
      console.error('播放失败:', e)
    }
  }
})
```

**(2) 播放模式切换**

三种播放模式通过 SVG 图标直观展示：

```javascript
const modeSvg = computed(() => {
  const svgs = {
    list: '<svg>...</svg>',    // 列表循环图标
    random: '<svg>...</svg>',  // 随机播放图标
    single: '<svg>...</svg>'   // 单曲循环图标（带数字 1）
  }
  return svgs[store.playMode]
})

const togglePlayMode = () => {
  const modes = ['list', 'random', 'single']
  const currentIndex = modes.indexOf(store.playMode)
  store.setPlayMode(modes[(currentIndex + 1) % modes.length])
}
```

**(3) 歌曲结束处理 (`onEnded`)**

```javascript
const onEnded = () => {
  if (store.isQueueMode) {
    // 队列模式：只播放队列中的歌曲，不放下一首
    const currentIndex = currentList.value.findIndex(s => s.id === currentSong.value?.id)
    if (currentIndex < currentList.value.length - 1) {
      playNext()
    }
  } else if (store.playMode === 'single') {
    // 单曲循环：回到开头重播
    audioRef.value.currentTime = 0
    audioRef.value.play()
  } else {
    // 列表循环/随机：自动下一首
    playNext()
  }
}
```

**(4) 睡眠定时器**

```javascript
const setSleepTimer = (minutes) => {
  if (sleepTimerId) clearTimeout(sleepTimerId)  // 清除旧定时器
  sleepTimerMinutes.value = minutes
  showSleepTimer.value = false
  sleepTimerId = setTimeout(() => {
    if (audioRef.value && !audioRef.value.paused) {
      audioRef.value.pause()    // 时间到 → 暂停播放
    }
    sleepTimerMinutes.value = 0
  }, minutes * 60 * 1000)
}
```

#### 2.3.3 全局键盘快捷键

文件：[App.vue](frontend/src/App.vue) (第 66-131 行)

```javascript
function handleKeydown(e) {
  // 输入框中不触发快捷键
  const tag = e.target.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return

  // 弹窗/抽屉打开时不触发
  const visibleDialog = document.querySelector('.el-dialog:not([style*="display: none"])')
  const visibleDrawer = document.querySelector('.el-drawer:not([style*="display: none"])')
  if (visibleDialog || visibleDrawer) return

  switch (e.code) {
    case 'Space':            // 空格 → 播放/暂停
      e.preventDefault()
      musicStore.togglePlay()
      break
    case 'ArrowLeft':        // ← → 快退5秒，Ctrl+← → 上一首
      e.preventDefault()
      if (e.ctrlKey || e.metaKey) {
        musicStore.playPrev()
      } else {
        const audioEl = document.querySelector('audio')
        if (audioEl) audioEl.currentTime = Math.max(0, audioEl.currentTime - 5)
      }
      break
    case 'ArrowRight':       // → → 快进5秒，Ctrl+→ → 下一首
      // ...类似逻辑
      break
    case 'ArrowUp':          // ↑ → 音量+5
      musicStore.setVolume(Math.min(100, musicStore.volume + 5))
      break
    case 'ArrowDown':        // ↓ → 音量-5
      musicStore.setVolume(Math.max(0, musicStore.volume - 5))
      break
    case 'KeyM':             // M → 静音切换
      const audioEl = document.querySelector('audio')
      if (audioEl) audioEl.muted = !audioEl.muted
      break
  }
}

// 第三个参数 true = 捕获阶段监听，比页面其他元素更早拿到事件
onMounted(() => {
  document.addEventListener('keydown', handleKeydown, true)
})
```

**设计要点：**
- `addEventListener` 第三个参数为 `true`（捕获阶段），这是解决 Element Plus 组件（如 `el-slider`）消耗方向键事件的关键
- 焦点在输入框时不触发，避免干扰正常打字
- 弹窗打开时不触发，避免在对话框里误触

### 2.4 后端：播放历史记录

文件：[controller/PlayHistoryController.java](backend/src/main/java/com/music/controller/PlayHistoryController.java)

```java
@RestController
@RequestMapping("/api/play-history")
public class PlayHistoryController {
    @Autowired
    private PlayHistoryService playHistoryService;

    // 记录播放 — 每次用户播放一首歌曲时调用
    @PostMapping("/{songId}")
    public Result record(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        playHistoryService.record(userId, songId);
        return Result.success();
    }

    // 获取用户的播放历史列表
    @GetMapping("/list")
    public Result list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(playHistoryService.getHistory(userId));
    }
}
```

---

## 3. 功能二：Web Audio API 音频可视化 + 均衡器

### 3.1 功能概述

- **频谱可视化**：实时显示音频频谱柱状图（Canvas 绘制）
- **5 段均衡器**：60Hz / 230Hz / 910Hz / 4kHz / 14kHz 五个频段独立调节
- **7 种预设音效**：标准、流行、摇滚、古典、电子、人声、重低音
- 音效偏好自动保存到 localStorage

### 3.2 技术原理

整个功能基于 **Web Audio API** 的标准管线：

```
HTMLAudioElement (audio标签)
    ↓
AudioContext.createMediaElementSource()  ← 从 <audio> 元素创建音频源节点
    ↓
AnalyserNode                            ← 频谱分析器（提取频域/时域数据）
    ↓
BiquadFilterNode × 5                    ← 5 个双二阶滤波器（均衡器核心）
    ↓
AudioContext.destination                ← 最终输出到扬声器
```

### 3.3 前端代码详解

文件：[components/Player.vue](frontend/src/components/Player.vue) (第 320-388 行)

#### 3.3.1 初始化音频管线

```javascript
const initAudioContext = () => {
  if (eqInitialized) return
  if (!audioRef.value) return

  destroyAudioContext()  // 先销毁旧的，避免节点残留

  try {
    // 1. 创建 AudioContext（浏览器音频处理的总控制器）
    audioContext = new (window.AudioContext || window.webkitAudioContext)()

    // 某些浏览器（尤其移动端）需要用户交互后才能启动 AudioContext
    if (audioContext.state === 'suspended') {
      audioContext.resume()
    }

    // 2. 从 <audio> 元素创建音频源节点
    //    注意：一个 audio 元素只能被 createMediaElementSource 一次
    sourceNode = audioContext.createMediaElementSource(audioRef.value)

    // 3. 创建频谱分析器节点
    //    fftSize=256 → 产生 128 个频域数据点
    //    值越大频率分辨率越高，但计算量也越大
    analyserNode.value = audioContext.createAnalyser()
    analyserNode.value.fftSize = 256

    // 4. 创建 5 个均衡器滤波器
    //    BiquadFilterNode 是一个双二阶滤波器
    const frequencies = [60, 230, 910, 4000, 14000]  // 5 个中心频率
    eqFilters = frequencies.map((freq, i) => {
      const filter = audioContext.createBiquadFilter()
      // 第一个用 lowshelf（低通搁架），最后一个用 highshelf（高通搁架）
      // 中间三个用 peaking（峰值滤波器）
      filter.type = i === 0 ? 'lowshelf' : i === frequencies.length - 1 ? 'highshelf' : 'peaking'
      filter.frequency.value = freq
      filter.Q.value = 1         // Q 值 = 带宽，值越大影响范围越窄
      filter.gain.value = 0      // 增益 (dB)，范围通常 -12 ~ +12
      return filter
    })

    // 5. 串联节点管线
    //    Source → Analyser → Filter1 → Filter2 → Filter3 → Filter4 → Filter5 → Destination
    sourceNode.connect(analyserNode.value)
    analyserNode.value.connect(eqFilters[0])
    for (let i = 0; i < eqFilters.length - 1; i++) {
      eqFilters[i].connect(eqFilters[i + 1])
    }
    eqFilters[eqFilters.length - 1].connect(audioContext.destination)

    eqInitialized = true
  } catch (e) {
    console.error('AudioContext init failed:', e)
    destroyAudioContext()
  }
}
```

#### 3.3.2 预设音效

```javascript
const eqPresets = [
  { key: 'normal', name: '标准', icon: '🎵', gains: [0, 0, 0, 0, 0] },
  { key: 'pop', name: '流行', icon: '🎤', gains: [1, 3, 5, 3, 1] },
  { key: 'rock', name: '摇滚', icon: '🎸', gains: [4, 2, -1, 2, 4] },
  { key: 'classical', name: '古典', icon: '🎻', gains: [3, 2, 0, 2, 3] },
  { key: 'electronic', name: '电子', icon: '🎹', gains: [4, 2, -2, 2, 5] },
  { key: 'vocal', name: '人声', icon: '🗣️', gains: [-2, 0, 4, 3, 0] },
  { key: 'bass', name: '重低音', icon: '🔊', gains: [6, 4, 0, -1, -2] },
]

const applyPreset = (key) => {
  activePreset.value = key
  localStorage.setItem('eq-preset', key)  // 持久化偏好
  const preset = eqPresets.find(p => p.key === key)
  if (preset) {
    preset.gains.forEach((g, i) => {
      eqBars.value[i].gain = g           // 更新 UI 滑块
      if (eqFilters[i]) eqFilters[i].gain.value = g  // 更新实际滤波器
    })
  }
}
```

**各预设的设计思路：**
- **流行**：全频段略微提升，突出中高频人声
- **摇滚**：提升低频和高频，削减中频（V 型曲线）
- **古典**：轻微提升低频和高频，保持自然
- **电子**：大幅提升低频和高频
- **人声**：削减低频，大幅提升中频（突出人声频段）
- **重低音**：大幅提升低频，削减高频

#### 3.3.3 Canvas 频谱可视化

文件：[components/AudioVisualizer.vue](frontend/src/components/AudioVisualizer.vue)

```javascript
function draw() {
  if (!canvasRef.value || !props.analyserNode) return
  if (!ctx) ctx = canvasRef.value.getContext('2d')

  const bufferLength = props.analyserNode.frequencyBinCount  // fftSize/2 = 128
  const dataArray = new Uint8Array(bufferLength)

  if (props.type === 'waveform') {
    // 时域数据：显示波形
    props.analyserNode.getByteTimeDomainData(dataArray)
  } else {
    // 频域数据：显示频谱（默认）
    props.analyserNode.getByteFrequencyData(dataArray)
  }

  ctx.clearRect(0, 0, props.width, props.height)

  const barWidth = (props.width / bufferLength) * 2.2
  let x = 0

  for (let i = 0; i < bufferLength; i++) {
    const value = dataArray[i] / 255          // 归一化到 0~1
    const barHeight = value * props.height

    // 通过色相变化实现渐变色彩效果
    const hue = 240 + (i / bufferLength) * 60  // 从蓝(240°)到青(300°)
    ctx.fillStyle = `hsla(${hue}, 80%, 60%, 0.85)`

    ctx.fillRect(x, props.height - barHeight, barWidth - 1, barHeight)
    x += barWidth + 1
  }

  // requestAnimationFrame 实现 60fps 动画循环
  animId = requestAnimationFrame(draw)
}
```

**关键 API 说明：**
- `getByteFrequencyData(dataArray)` → 获取频域数据（0-255 的整数，代表各频率的能量）
- `getByteTimeDomainData(dataArray)` → 获取时域波形数据
- `requestAnimationFrame(draw)` → 浏览器原生动画循环，自动适配刷新率

---

## 4. 功能三：LRC 歌词解析与桌面歌词

### 4.1 功能概述

- LRC 歌词文件解析（支持翻译行）
- 根据播放进度自动高亮当前歌词行
- 歌词面板：点击某行歌词跳转到对应播放位置
- 歌词偏移调节（±5 秒范围）
- 桌面歌词（可拖拽的悬浮窗，始终置顶）

### 4.2 LRC 歌词格式

```
[00:00.00]歌曲名
[00:05.50]第一句歌词
[00:05.50]First line translation    ← 同一时间戳的翻译行
[00:12.30]第二句歌词
[00:12.30]Second line translation
```

### 4.3 前端代码详解

文件：[composables/useLyric.js](frontend/src/composables/useLyric.js)

#### 4.3.1 歌词解析

```javascript
const parsedLyrics = computed(() => {
  if (!lyricContent.value) return []
  const lines = lyricContent.value.split('\n')
  const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/

  // 使用 Map 按时间戳分组（同一时间戳的翻译行会合并到同一组）
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

    const key = rawTime.toFixed(3)  // 用时间戳作为 Map 的 key
    if (!grouped.has(key)) {
      // 第一次遇到该时间戳 → 作为原文
      grouped.set(key, { time: rawTime, text, translation: '' })
    } else {
      // 第二次遇到该时间戳 → 作为翻译
      grouped.get(key).translation = text
    }
  }

  // 转换为数组并应用偏移
  const result = []
  for (const entry of grouped.values()) {
    result.push({
      time: entry.time + offset.value,  // + 偏移量
      text: entry.text,
      translation: entry.translation
    })
  }
  return result
})
```

**关键设计：**
- 用 `Map` 按时间戳分组，自动将同一时间的第二行识别为翻译
- `padEnd(3, '0')` 统一毫秒位数为 3 位（LRC 格式中毫秒可能是 2 位或 3 位）

#### 4.3.2 当前歌词行定位

```javascript
const currentLyricIndex = computed(() => {
  if (!parsedLyrics.value.length) return -1
  const currentTime = store.currentTime  // 来自 Pinia Store 的实时播放位置
  let index = -1
  for (let i = 0; i < parsedLyrics.value.length; i++) {
    if (parsedLyrics.value[i].time <= currentTime) {
      index = i   // 找到最后一个时间 ≤ 当前播放时间的歌词行
    } else {
      break
    }
  }
  return index
})
```

#### 4.3.3 桌面歌词

文件：[components/Player.vue](frontend/src/components/Player.vue) (第 176-225 行)

桌面歌词通过 `Teleport` 渲染到 `<body>` 层级，使用鼠标拖拽实现自由定位：

```javascript
// 显示当前行 + 前一行 + 后一行 + 后两行（共 4 行）
const desktopLyricLines = computed(() => {
  const idx = currentLyricIndex.value
  const lines = []
  if (idx > 0) {
    lines.push({ type: 'prev', text: parsedLyrics.value[idx - 1]?.text })
  }
  lines.push({ type: 'current', text: parsedLyrics.value[idx]?.text })
  if (idx + 1 < parsedLyrics.value.length) {
    lines.push({ type: 'next', text: parsedLyrics.value[idx + 1]?.text })
  }
  if (idx + 2 < parsedLyrics.value.length) {
    lines.push({ type: 'far', text: parsedLyrics.value[idx + 2]?.text })
  }
  return lines
})

// 拖拽实现
const startDrag = (e) => {
  if (e.target.closest('.desktop-lyrics-actions')) return  // 操作按钮区域不拖拽
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
```

桌面歌词的视觉效果：
- 当前行：24px 字号，全亮白色 + 紫色发光文字阴影
- 前行：15px 字号，40% 透明度
- 后行：16px 字号，60% 透明度
- 远行：14px 字号，30% 透明度

---

## 5. 功能四：用户认证体系 (JWT + 验证码 + 邮箱)

### 5.1 功能概述

- **注册**：用户名 + 密码 + 图形验证码
- **登录**：用户名 + 密码 → 返回 JWT Token
- **JWT 认证**：每次请求携带 `Authorization: Bearer <token>`
- **图形验证码**：Google Kaptcha 生成，存入 Session
- **邮箱验证码**：注册绑定邮箱、找回密码
- **BCrypt 密码加密**：不可逆存储
- **前后端双重权限守卫**：路由守卫 + 拦截器

### 5.2 认证流程

```
用户登录
  ↓
前端 POST /api/auth/login { username, password }
  ↓
后端 AuthController.login()
  ├─ 查数据库验证用户名
  ├─ BCrypt 验证密码
  ├─ 检查账号状态（是否被禁用）
  ├─ 生成 JWT Token（包含 userId + username）
  └─ 返回 { token, userInfo }
  ↓
前端 store.setToken(token)
  ├─ 存入 localStorage
  └─ Axios 拦截器自动在后续请求中携带
  ↓
每次请求 → JwtInterceptor 解析 Token → 注入 userId 到 request
```

### 5.3 前端代码详解

#### 5.3.1 Axios 请求拦截器

文件：[api/request.js](frontend/src/api/request.js)

```javascript
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',      // Vite 代理到后端 8080
  timeout: 10000        // 10 秒超时
})

// 请求拦截器：自动在请求头注入 JWT Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`  // 标准 JWT 格式
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      // Token 过期 → 清空状态 → 跳登录页
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
      ElMessage.error(res.message || '请先登录')
      return Promise.reject(new Error(res.message))
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res  // 返回 response.data
  },
  error => {
    if (error.response?.status === 401) {
      // HTTP 状态码 401 → 同样处理
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)
```

#### 5.3.2 前端路由守卫

文件：[router/index.js](frontend/src/router/index.js) (第 123-139 行)

```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  if (to.meta.requiresAuth && !token) {
    // 需要登录但没 token → 强制跳登录页
    next('/login')
  } else if (to.meta.guest && token) {
    // 游客页面（登录/注册）但已登录 → 不需要再看，跳首页
    next('/')
  } else if (to.meta.requiresAdmin && (!user || user.role !== 'ADMIN')) {
    // 需要管理员但不是 → 跳首页
    next('/')
  } else {
    next()  // 放行
  }
})
```

**路由元信息定义：**

```javascript
{ path: '/login', meta: { guest: true } }                    // 仅游客可访问
{ path: '/my-playlists', meta: { requiresAuth: true } }       // 需要登录
{ path: '/admin/dashboard', meta: { requiresAuth: true, requiresAdmin: true } }  // 需要管理员
```

### 5.4 后端代码详解

#### 5.4.1 JWT 工具类

```java
public class JwtUtil {
    private static final String SECRET = "music-app-secret-key-2024";
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000;  // 7 天过期

    // 生成 Token
    public static String generateToken(Long userId, String username) {
        return JWT.create()
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE))
            .sign(Algorithm.HMAC256(SECRET));
    }

    // 验证并解析 Token
    public static DecodedJWT verify(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);
    }
}
```

#### 5.4.2 JWT 拦截器

```java
@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                DecodedJWT jwt = JwtUtil.verify(token.substring(7));
                // 将 userId 注入 request，后续 Controller 可直接获取
                request.setAttribute("userId", jwt.getClaim("userId").asLong());
                return true;
            } catch (Exception e) {
                // Token 无效
            }
        }
        response.setStatus(401);
        return false;
    }
}
```

#### 5.4.3 登录接口

```java
@PostMapping("/login")
public Result login(@RequestBody LoginRequest loginReq) {
    // 1. 查用户
    User user = userRepository.findByUsername(loginReq.getUsername());
    if (user == null) return Result.error("用户名或密码错误");

    // 2. BCrypt 验证密码
    if (!passwordEncoder.matches(loginReq.getPassword(), user.getPassword())) {
        return Result.error("用户名或密码错误");
    }

    // 3. 检查状态
    if (user.getStatus() == 0) return Result.error("账号已被禁用");

    // 4. 生成 Token
    String token = JwtUtil.generateToken(user.getId(), user.getUsername());

    // 5. 返回
    LoginResponse resp = new LoginResponse();
    resp.setToken(token);
    resp.setUserId(user.getId());
    resp.setUsername(user.getUsername());
    resp.setRole(user.getRole());
    return Result.success(resp);
}
```

#### 5.4.4 管理员权限注解 + 拦截器

```java
// 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {}

// 管理员拦截器
@Component
public class AdminInterceptor implements HandlerInterceptor {
    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 只拦截有 @RequireAdmin 注解的方法
        if (handler instanceof HandlerMethod) {
            RequireAdmin annotation = ((HandlerMethod) handler).getMethodAnnotation(RequireAdmin.class);
            if (annotation == null) return true;  // 不需要管理员 → 放行

            Long userId = (Long) request.getAttribute("userId");
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !"ADMIN".equals(user.getRole())) {
                response.setStatus(403);
                return false;
            }
        }
        return true;
    }
}

// 在 Controller 中使用
@RequireAdmin
@GetMapping("/admin/users")
public Result getUsers() { ... }
```

#### 5.4.5 图形验证码

```java
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {
    @GetMapping("/generate")
    public void generate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Kaptcha 生成 4 位验证码图片
        String text = captchaProducer.createText();
        BufferedImage image = captchaProducer.createImage(text);

        // 存入 Session（后续注册时比对）
        request.getSession().setAttribute("captcha", text);

        // 输出为 PNG 图片
        response.setContentType("image/png");
        ImageIO.write(image, "png", response.getOutputStream());
    }
}
```

---

## 6. 功能五：多维搜索系统

### 6.1 功能概述

搜索功能支持 **5 个维度** 的全局搜索：

| 维度 | 搜索方式 | 说明 |
|------|---------|------|
| 歌曲 | SQL LIKE 模糊匹配 | 匹配歌曲名、歌手名、专辑名 |
| 歌单 | SQL LIKE 模糊匹配 | 匹配歌单名称 |
| 歌手 | SQL LIKE 模糊匹配 | 匹配歌手名 |
| 帖子 | SQL LIKE 模糊匹配 | 匹配帖子标题和内容 |
| 用户 | SQL LIKE 模糊匹配 | 匹配用户名和昵称 |

辅以：**热门搜索** + **搜索历史** + **自动补全下拉**

### 6.2 前端代码详解

文件：[views/Search.vue](frontend/src/Search.vue)

#### 6.2.1 搜索主逻辑

```javascript
const handleSearch = async () => {
  if (!keyword.value.trim()) return
  hasSearched.value = true
  currentPage.value = 1

  // 记录搜索历史（异步，不阻塞主搜索）
  if (userStore.userInfo?.userId) {
    recordSearch(keyword.value.trim()).catch(() => {})
  }

  // 同步更新 URL 参数，方便分享搜索结果
  router.push({ query: { keyword: keyword.value.trim() } })

  const kw = keyword.value.trim()

  // 并发请求所有维度（实际上这里是顺序请求，可以优化为 Promise.all）
  const songRes = await searchSongs(kw)
  songResults.value = songRes.data || []
  store.setPlayList(songResults.value)  // 搜索结果直接设为播放列表

  const plRes = await searchPlaylists(kw)
  playlistResults.value = plRes.data || []

  const userRes = await searchUsers(kw)
  userResults.value = userRes.data?.list || []

  const singerRes = await searchSingers(kw)
  singerResults.value = singerRes.data || []

  const postRes = await searchPosts(kw)
  postResults.value = postRes.data || []
}
```

#### 6.2.2 搜索下拉框（热门搜索 + 搜索历史）

```html
<div v-if="showDropdown" class="search-dropdown">
  <!-- 热门搜索区域 -->
  <div v-if="hotSearch.length" class="dropdown-section">
    <div class="dropdown-title">热门搜索</div>
    <div class="hot-tags">
      <div v-for="(tag, idx) in hotSearch" :key="tag"
           class="hot-tag" @click="searchByTag(tag)">
        <span class="hot-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
        {{ tag }}
      </div>
    </div>
  </div>
  <!-- 搜索历史区域 -->
  <div v-if="searchHistory.length" class="dropdown-section">
    <div class="dropdown-title">
      搜索历史
      <div class="clear-btn" @click="handleClearHistory">清空</div>
    </div>
    ...
  </div>
</div>
```

#### 6.2.3 前端分页

```javascript
const currentPage = ref(1)
const pageSize = 12

const paginatedSongs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return songResults.value.slice(start, start + pageSize)
})
```

分页在前端通过 `computed` 属性实现（搜索结果通常不超过几百条，前端分页足够快）。

### 6.3 后端代码详解

#### 6.3.1 歌曲搜索

```java
@GetMapping("/search/songs")
public Result searchSongs(@RequestParam String keyword) {
    // JPA 原生查询：匹配歌名、歌手、专辑三个字段
    List<Song> songs = songRepository.findByNameContainingOrSingerContainingOrAlbumContaining(
        keyword, keyword, keyword);
    return Result.success(songs);
}
```

#### 6.3.2 热门搜索

热门搜索基于播放量最多的歌曲名，取前 N 个：

```java
@GetMapping("/search/hot")
public Result hotSearch(@RequestParam(defaultValue = "10") int limit) {
    List<String> hotKeywords = searchHistoryService.getHotKeywords(limit);
    return Result.success(hotKeywords);
}
```

#### 6.3.3 搜索历史

```java
@PostMapping("/search/record")
public Result record(@RequestParam String keyword, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    searchHistoryService.record(userId, keyword);
    return Result.success();
}
```

---

## 7. 功能六：音乐社区 (帖子 + 评论 + 点赞 + 关注)

### 7.1 功能概述

一个完整的社区功能，包括：

- 发布帖子（支持文字 + 多图片 + 关联歌曲/歌单）
- 帖子点赞/取消点赞
- 评论帖子
- 评论点赞
- 关注/取关用户
- 分享帖子（复制链接）
- 帖子搜索

### 7.2 数据模型

```
CommunityPost (帖子)
  ├─ id, title, content, coverUrl, imageUrls
  ├─ userId (发帖人)
  ├─ relatedSongId (关联歌曲)
  ├─ relatedPlaylistId (关联歌单)
  ├─ likeCount, commentCount
  └─ createTime

PostLike (帖子点赞)
  ├─ id, postId, userId
  └─ createTime

Comment (评论)
  ├─ id, content
  ├─ targetType ("song" | "post" | "playlist")
  ├─ targetId
  ├─ userId, parentId (支持楼中楼回复)
  └─ createTime

UserFollow (用户关注)
  ├─ id, followerId, followingId
  └─ createTime
```

### 7.3 前端代码详解

文件：[views/Community.vue](frontend/src/views/Community.vue)

#### 7.3.1 帖子点赞（Toggle 模式）

```javascript
const handleLike = async (post) => {
  if (!userStore.userInfo?.userId) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await likePost(post.id)
    const liked = res.data.liked      // 后端返回点赞后的状态
    post.liked = liked
    post.likeCount += liked ? 1 : -1  // 本地更新计数（乐观更新）
    ElMessage.success(liked ? '点赞成功' : '已取消点赞')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}
```

#### 7.3.2 关联歌曲卡片

```html
<!-- 在帖子中嵌入可播放的歌曲卡片 -->
<div v-if="post.relatedSong" class="related-music-card" @click.stop="playRelatedSong(post.relatedSong)">
  <div class="rm-cover">
    <img v-if="post.relatedSong.coverUrl" :src="post.relatedSong.coverUrl" />
    <div class="rm-play-icon always-show">
      <el-icon :size="20" color="#fff"><VideoPlay /></el-icon>
    </div>
  </div>
  <div class="rm-info">
    <div class="rm-name">{{ post.relatedSong.name }}</div>
    <div class="rm-singer">{{ post.relatedSong.singer }}</div>
  </div>
  <div class="rm-play-btn">
    <el-icon><VideoPlay /></el-icon> 播放
  </div>
</div>
```

#### 7.3.3 多图片上传

```javascript
const triggerUpload = () => {
  fileInputRef.value?.click()   // 触发隐藏的 <input type="file">
}

const handleFileSelect = async (e) => {
  const files = Array.from(e.target.files)
  for (const file of files) {
    // 最多 9 张
    if (createForm.value.imageList.length >= 9) break
    // 只接受图片
    if (!file.type.startsWith('image/')) continue
    // 大小限制 5MB
    if (file.size / 1024 / 1024 > 5) {
      ElMessage.error(`${file.name} 超过5MB`)
      continue
    }
    try {
      const res = await uploadPostImage(file)  // 上传到后端
      createForm.value.imageList.push(res.data) // 拿到返回的 URL
    } catch (err) {
      ElMessage.error(`${file.name} 上传失败`)
    }
  }
  e.target.value = ''   // 清空 input，允许重复选择相同文件
}
```

### 7.4 后端代码详解

文件：[controller/CommunityController.java](backend/src/main/java/com/music/controller/CommunityController.java)

```java
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    // 获取帖子列表 — 按时间倒序，带分页
    @GetMapping("/posts")
    public Result getPosts(@RequestParam(defaultValue = "50") int limit) {
        List<CommunityPost> posts = communityService.getPosts(limit);
        return Result.success(posts);
    }

    // 发布帖子
    @PostMapping("/posts")
    public Result createPost(@RequestBody CommunityPost post, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        post.setUserId(userId);
        communityService.createPost(post);
        return Result.success(post);
    }

    // 点赞/取消点赞（Toggle 模式）
    @PostMapping("/posts/{postId}/like")
    public Result likePost(@PathVariable Long postId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 查询是否已经点赞
        Optional<PostLike> existing = postLikeRepository.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());  // 已点赞 → 取消
            liked = false;
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            postLikeRepository.save(like);              // 未点赞 → 点赞
            liked = true;
        }

        // 更新帖子的点赞计数
        communityService.updateLikeCount(postId);
        return Result.success(Map.of("liked", liked));
    }
}
```

**设计要点：**
- 点赞接口使用 **Toggle 模式**：同一个接口，有则删、无则增，前端不需要知道当前状态
- 点赞后触发通知：点赞操作会通过 `NotificationService` 向帖子作者发送通知

---

## 8. 功能七：管理员后台系统

### 8.1 功能概述

管理员后台提供 9 个子功能模块：

| 模块 | 功能 |
|------|------|
| 数据概览 | 用户/歌曲/歌单/播放量统计 + 近 7/30 日播放趋势图 + Top 10 热门歌曲 |
| 用户管理 | 列表展示、搜索、角色分配、启用/禁用、删除 |
| 歌曲管理 | 搜索、新增/编辑、上下架、删除 |
| 歌单管理 | 审核、上下架、删除 |
| 评论管理 | 查看、删除、恢复 |
| 社区管理 | 帖子管理 |
| 公告管理 | 发布/编辑/删除系统公告 |
| 操作日志 | 记录管理员的所有操作（AOP 实现） |
| 系统设置 | 网站名称、Logo、备案信息等 |

### 8.2 权限控制

**双层控制：**

```
前端路由守卫 (router/index.js)
  → meta: { requiresAdmin: true }
  → 非 ADMIN 角色的用户看不到管理页面入口

后端拦截器 (AdminInterceptor)
  → 检查接口是否有 @RequireAdmin 注解
  → 从数据库验证用户角色是否为 ADMIN
  → 返回 403 阻止非管理员调用
```

### 8.3 后端代码详解

#### 8.3.1 数据概览 API

```java
@RequireAdmin
@GetMapping("/admin/dashboard")
public Result dashboard() {
    Map<String, Object> data = new HashMap<>();

    // 统计卡片
    data.put("userCount", userRepository.count());
    data.put("songCount", songRepository.count());
    data.put("playlistCount", playlistRepository.count());
    data.put("todayPlayCount", playHistoryRepository.countToday());

    // 近 7 日播放趋势（每天一条）
    data.put("dailyPlays", playHistoryRepository.countByDay(7));

    // Top 10 热门歌曲
    data.put("topSongs", playHistoryRepository.findTopSongs(10));

    // 内容分布（饼图）
    data.put("songByGenre", songRepository.countByGenre());

    return Result.success(data);
}
```

#### 8.3.2 AOP 操作日志记录

```java
// 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogAnnotation {
    String value();  // 操作描述，如 "删除用户"、"修改角色"
}

// AOP 切面
@Aspect
@Component
public class OperationLogAspect {
    @Autowired
    private OperationLogService logService;

    @AfterReturning("@annotation(annotation)")  // 方法成功返回后执行
    public void afterReturning(JoinPoint joinPoint, OperationLogAnnotation annotation) {
        // 获取请求中的 userId
        HttpServletRequest request = ((ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes()).getRequest();
        Long userId = (Long) request.getAttribute("userId");

        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        String details = JSON.toJSONString(args);

        // 记录日志
        logService.log(userId, annotation.value(), details);
    }
}

// 使用示例
@OperationLogAnnotation("修改用户角色")
@PutMapping("/admin/users/{id}/role")
public Result updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
    // ...
}
```

#### 8.3.3 ECharts 图表渲染

文件：[views/admin/Dashboard.vue](frontend/src/views/admin/Dashboard.vue)

```javascript
// 播放趋势折线图
function renderPlayTrend() {
  if (!playTrendRef.value) return
  const chart = echarts.init(playTrendRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dailyPlays.value.map(d => d.date) },
    yAxis: { type: 'value' },
    series: [{
      data: dailyPlays.value.map(d => d.count),
      type: 'line',
      smooth: true,
      areaStyle: { /* 渐变填充 */ },
      lineStyle: { color: '#818cf8', width: 2 }
    }]
  })
}

// 内容分布饼图
function renderContentDist() {
  const chart = echarts.init(contentDistRef.value)
  chart.setOption({
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],   // 环形图
      data: [/* 各类型数量 */]
    }]
  })
}
```

---

## 9. 功能八：Canvas 动态背景

### 9.1 功能概述

页面底部有一个动态的 Canvas 背景，随主题变化呈现不同的视觉效果：
- **暗色主题**：64 根彩色柱状图 + 2 层波浪线，随"时间"参数持续变化
- **亮色主题**：4 层柔和波浪线，颜色更淡

### 9.2 代码详解

文件：[components/DynamicBackground.vue](frontend/src/components/DynamicBackground.vue)

```javascript
const WAVE_COUNT = 4
const BAR_COUNT = 64
let time = 0  // 全局时间变量，每帧递增

// 暗色模式：柱状图 + 波浪
function drawDarkMode() {
  ctx.clearRect(0, 0, w, h)
  const barWidth = Math.max(w / BAR_COUNT, 4)

  for (let i = 0; i < BAR_COUNT; i++) {
    const x = i * (barWidth + 2)

    // n1, n2, n3：三个不同频率和相位的正弦波
    // 叠加后产生不规则但有节奏的高度变化
    const n1 = Math.sin(i * 0.15 + time * 0.7)
    const n2 = Math.sin(i * 0.08 + time * 0.4 + 1)
    const n3 = Math.sin(i * 0.25 + time * 1.1 + 2)

    // 将三个波叠加取平均 → 得到 0~0.22 范围的值 → 映射为高度
    const barH = Math.max(h * (0.08 + ((n1 + n2 + n3) / 3) * 0.22), 4)
    const y = h - barH

    // 色相根据位置和正弦值动态变化 → 产生流光效果
    const hue = 240 + (i / BAR_COUNT) * 60 + Math.sin(time * 0.3 + i * 0.05) * 20
    const lightness = 50 + Math.sin(time * 0.5 + i * 0.08) * 15
    const alpha = 0.12 + Math.abs(n1) * 0.12

    // 绘制圆角矩形
    ctx.beginPath()
    const r = Math.min(barWidth / 2, barH / 2)
    // ... arcTo 绘制圆角路径 ...

    // 添加渐变（顶部亮 → 底部透明消失）
    const grad = ctx.createLinearGradient(x, y, x, h)
    grad.addColorStop(0, `hsla(${hue},80%,${lightness}%,${alpha})`)
    grad.addColorStop(1, `hsla(${hue},80%,${lightness}%,0)`)
    ctx.fillStyle = grad
    ctx.fill()
  }
  // ... 再加 2 层波浪 ...
}

// 动画循环
function animate() {
  time += 0.016   // 约等于 1/60 秒
  if (themeStore.isDark) {
    drawDarkMode()
  } else {
    drawLightMode()
  }
  animId = requestAnimationFrame(animate)  // 每帧都画
}
```

**视觉原理：**
- `Math.sin(i * freq + time * speed)` — 正弦波在不同频率和速度下叠加
- `time += 0.016` — 每帧增量，所有动画参数依赖 time，确保流畅连续变化
- HSL 颜色空间 — 通过改变 hue（色相）和 lightness（亮度）实现流光效果
- 渐变透明度 — 从顶部亮到底部透明，柱状图看起来"悬浮"在页面上

---

## 附录：关键技术知识点总结

### A. Vue 3 组合式 API 核心概念

| 概念 | 说明 | 本项目中的例子 |
|------|------|--------------|
| `ref()` | 创建响应式引用 | `const currentSong = ref(null)` |
| `computed()` | 计算属性（自动缓存） | `const progressPercent = computed(() => ...)` |
| `watch()` | 监听数据变化 | `watch(() => store.currentSong, async (newSong) => ...)` |
| `defineStore()` | Pinia 状态管理 | `useMusicStore`, `useUserStore` |
| `Teleport` | 将内容渲染到 DOM 其他位置 | 歌词面板渲染到 `<body>` |
| `<script setup>` | 编译时语法糖 | 所有 `.vue` 文件 |
| `useXxx()` | 组合式函数约定 | `useLyric()`, `useAudioAnalyser()` |

### B. Spring Boot 核心概念

| 概念 | 说明 | 本项目中的例子 |
|------|------|--------------|
| `@RestController` | REST API 控制器 | 所有 Controller 类 |
| `@Autowired` | 依赖注入 | 注入 Service/Repository |
| `JpaRepository` | ORM 数据访问 | 所有 Repository 接口 |
| `HandlerInterceptor` | 请求拦截器 | JwtInterceptor, AdminInterceptor |
| `@Aspect` | AOP 切面编程 | OperationLogAspect |
| `@RequireAdmin` | 自定义注解 | 管理员权限控制 |

### C. Web Audio API 核心概念

| API | 说明 |
|-----|------|
| `AudioContext` | 音频处理环境的"总控制器" |
| `createMediaElementSource()` | 从 `<audio>` 元素创建音频源 |
| `createAnalyser()` | 创建频谱分析器 |
| `createBiquadFilter()` | 创建双二阶滤波器（均衡器核心） |
| `getByteFrequencyData()` | 获取频域数据 |
| `getByteTimeDomainData()` | 获取时域波形数据 |

---

> 本文档涵盖了本音乐网站项目中最具代表性的 8 个功能模块，每个功能都从前端 Vue 组件到后端 Spring Boot 接口进行了完整的代码级拆解。
> 建议学习顺序：先理解播放器（核心）→ 认证体系（基础）→ 搜索/社区（业务）→ 可视化/动态背景（进阶）→ 管理员系统（完整闭环）。
