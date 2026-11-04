<template>
  <div class="batch-import">
    <div class="page-header">
      <div>
        <h2>批量导入</h2>
        <p class="sub">从文件夹批量选择歌曲，自动读取标签、配对这些歌词，一次性建好</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click.prevent="audioPicker.click()">
          <el-icon><FolderOpened /></el-icon> 选音乐文件夹
        </button>
        <button class="btn btn-primary" @click.prevent="lyricPicker.click()">
          <el-icon><Document /></el-icon> 选歌词文件夹
        </button>
        <button class="btn btn-ghost" @click="resetAll">清空</button>
      </div>
    </div>

    <!-- 隐藏的文件夹选择器：webkitdirectory 可以整目录选 -->
    <input ref="audioPicker" type="file" multiple webkitdirectory directory style="display: none" @change="onPickAudio" />
    <input ref="lyricPicker" type="file" multiple webkitdirectory directory style="display: none" @change="onPickLyric" />

    <!-- 拖拽区 -->
    <div
      class="drop-zone"
      :class="{ active: dragOver }"
      @dragover.prevent="dragOver = true"
      @dragleave.prevent="dragOver = false"
      @drop.prevent="onDrop"
    >
      <el-icon :size="26"><UploadFilled /></el-icon>
      <p class="drop-title">也可以直接把文件拖到这里</p>
      <p class="drop-hint">
        支持 .mp3 .flac .m4a .aac .wav 和 .lrc —— 音频与歌词可以一起拖，会自动按歌名配对
      </p>
    </div>

    <!-- 解析进度 -->
    <div v-if="parsing" class="bar-wrap">
      <div class="bar-label">{{ parseLabel }}（{{ parseCurrent }}/{{ parseTotal }}）</div>
      <div class="bar"><div class="bar-inner" :style="{ width: parsePercent + '%' }"></div></div>
    </div>

    <!-- 统计 -->
    <div v-if="rows.length" class="stats">
      <span>音频 <b>{{ rows.length }}</b></span>
      <span>已配对歌词 <b class="ok">{{ lyricMatched }}</b></span>
      <span>缺歌词 <b class="warn">{{ rows.length - lyricMatched }}</b></span>
      <span>待导入 <b>{{ selectedCount }}</b></span>
      <span class="tip">.aac 在部分浏览器无法播放，建议先转成 mp3</span>
    </div>

    <!-- 列表 -->
    <div v-if="rows.length" class="data-card">
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 40px"><input type="checkbox" :checked="allSelected" @change="toggleAll" /></th>
              <th style="min-width: 180px">歌名</th>
              <th style="min-width: 130px">歌手</th>
              <th style="min-width: 120px">专辑</th>
              <th style="width: 90px">时长</th>
              <th style="min-width: 130px">歌词</th>
              <th style="width: 70px">封面</th>
              <th style="min-width: 150px">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.key" :class="row.status">
              <td><input v-model="row.selected" type="checkbox" /></td>
              <td><input v-model="row.title" class="cell-input" /></td>
              <td><input v-model="row.artist" class="cell-input" /></td>
              <td><input v-model="row.album" class="cell-input" /></td>
              <td class="td-sub">{{ fmtDuration(row.duration) }}</td>
              <td>
                <span v-if="row.lyricText" class="ok">✓ {{ row.lyricName }}</span>
                <button v-else class="btn btn-sm btn-ghost" @click="pickLyricFor(row)">选择 .lrc</button>
              </td>
              <td class="td-sub">{{ row.coverBlob ? '✓' : '—' }}</td>
              <td class="td-sub">{{ statusText(row) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty">
      <p>还没有选择文件</p>
      <p class="sub">点上面的「选音乐文件夹」把 <code>E:\音乐</code> 选进来，再选 <code>E:\音乐歌词</code></p>
    </div>

    <!-- 导入区 -->
    <div v-if="rows.length" class="footer">
      <div class="footer-left">
        <span v-if="!importing" class="tip">
          将导入 {{ selectedCount }} 首 · 串行上传，过程中请不要关闭页面
        </span>
        <div v-else class="bar-wrap" style="flex: 1">
          <div class="bar-label">{{ importLabel }}（{{ importCurrent }}/{{ importTotal }}）</div>
          <div class="bar"><div class="bar-inner blue" :style="{ width: importPercent + '%' }"></div></div>
        </div>
      </div>
      <div class="footer-right">
        <button class="btn btn-ghost" :disabled="importing" @click="retryFailed">重试失败项</button>
        <button class="btn btn-primary" :disabled="importing || !selectedCount" @click="startImport">
          {{ importing ? '导入中...' : `开始导入 ${selectedCount} 首` }}
        </button>
      </div>
    </div>

    <!-- 单行补选歌词 -->
    <input ref="singleLyricPicker" type="file" accept=".lrc" style="display: none" @change="onSingleLyricPicked" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { FolderOpened, Document, UploadFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { uploadAudio, uploadCover, getAudioDuration } from '@/api/song'
import { readId3FromFile } from '@/utils/id3'

const AUDIO_EXT = ['mp3', 'flac', 'm4a', 'aac', 'wav', 'ogg']

const audioPicker = ref(null)
const lyricPicker = ref(null)
const singleLyricPicker = ref(null)
const dragOver = ref(false)

const rows = ref([])
const lyricPool = ref([])        // { file, text, title, artist, used }
const parsing = ref(false)
const parseLabel = ref('')
const parseCurrent = ref(0)
const parseTotal = ref(0)
const parsePercent = computed(() => parseTotal.value ? Math.round(parseCurrent.value / parseTotal.value * 100) : 0)

const importing = ref(false)
const importLabel = ref('')
const importCurrent = ref(0)
const importTotal = ref(0)
const importPercent = computed(() => importTotal.value ? Math.round(importCurrent.value / importTotal.value * 100) : 0)

const selectedCount = computed(() => rows.value.filter(r => r.selected && r.status !== 'done').length)
const lyricMatched = computed(() => rows.value.filter(r => r.lyricText).length)
const allSelected = computed(() => rows.value.length > 0 && rows.value.every(r => r.selected))

let pendingLyricRow = null

// ==================== 工具函数 ====================
const extOf = (name) => (name.toLowerCase().split('.').pop() || '')
const baseNoExt = (name) => name.replace(/\.[^.]+$/, '')

const decodeEntities = (s) => s
  .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
  .replace(/&quot;/g, '"').replace(/&#39;/g, "'")

/**
 * 比对用的归一化
 *   全角转半角 → 去掉括号及其内容 → 去空格与标点 → 小写
 *
 * 为什么要去掉括号内容：
 *   ID3 标题常是 `Angels`，而歌词文件叫 `Angels (Radio Edit)-...-歌词.lrc`，
 *   不去掉括号就会出现「本该配上却配不上」。
 */
const norm = (s) => decodeEntities(String(s || ''))
  .toLowerCase()
  .replace(/（[^）]*）|\([^)]*\)|【[^】]*】|\[[^\]]*\]/g, '')
  .replace(/[\uff01-\uff5e]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0xfee0))
  .replace(/[\s\-_·.,:;!?'"“”‘’&+~`|/\\]/g, '')
  .trim()

/** 从 `歌手 - 歌名.mp3` / `歌名.mp3` 猜歌名歌手（仅在文件没有 ID3 标签时使用） */
const parseAudioFileName = (fileName) => {
  let base = decodeEntities(baseNoExt(fileName))
  base = base.replace(/^\s*\d{1,3}\s*[.\-_、)]\s*/, '')

  // 网易云那种纯 ID 文件名（如 002OKIox28ad9a）不当歌名解析
  if (/^[A-Za-z0-9]{8,}$/.test(base)) return { title: base, artist: '' }

  // 优先认「两边都有空格」的分隔符：`歌手 - 歌名`
  const spaced = base.match(/^(.+?)\s+[-–—－]\s+(.+)$/)
  if (spaced) return { artist: spaced[1].trim(), title: spaced[2].trim() }

  // 没有空格时按「最后一个连字符」切：`HOYO-Mix-希望有羽毛和翅膀` → 歌手 HOYO-Mix
  // 用最后一个而不是第一个，避免把歌手名里的连字符当分隔符
  const lastDash = base.lastIndexOf('-')
  if (lastDash > 0 && lastDash < base.length - 1) {
    return { artist: base.slice(0, lastDash).trim(), title: base.slice(lastDash + 1).trim() }
  }

  return { title: base.trim(), artist: '' }
}

/** 从 `歌名-歌手-歌词.lrc` 拆出歌名与歌手 */
const parseLyricFileName = (fileName) => {
  let base = decodeEntities(baseNoExt(fileName))
  base = base.replace(/\s*[-–—－]?\s*歌词\s*$/, '').trim()
  const parts = base.split(/\s*[-–—－]\s*/).filter(Boolean)
  if (parts.length >= 2) {
    return { title: parts[0].trim(), artist: parts.slice(1).join(' & ').trim() }
  }
  return { title: base.trim(), artist: '' }
}

const readTextFile = async (file) => {
  const buf = await file.arrayBuffer()
  let text = new TextDecoder('utf-8').decode(buf)
  if (text.includes('\uFFFD')) {
    try { text = new TextDecoder('gbk').decode(buf) } catch (e) { /* ignore */ }
  }
  return text.replace(/^\uFEFF/, '')
}

const fmtDuration = (sec) => {
  if (!sec) return '—'
  const m = Math.floor(sec / 60)
  const s = Math.round(sec % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

const statusText = (row) => {
  if (row.status === 'done') return '✅ 已导入'
  if (row.status === 'failed') return '❌ ' + (row.message || '失败')
  if (row.status === 'uploading') return '上传中…'
  return row.lyricText ? '待导入（含歌词）' : '待导入（无歌词）'
}

// ==================== 选择文件 ====================
const onPickAudio = async (e) => {
  await addAudioFiles(e.target.files)
  e.target.value = ''
}
const onPickLyric = async (e) => {
  await addLyricFiles(e.target.files)
  e.target.value = ''
}

const onDrop = async (e) => {
  dragOver.value = false
  const files = Array.from(e.dataTransfer?.files || [])
  const audio = files.filter(f => AUDIO_EXT.includes(extOf(f.name)))
  const lyric = files.filter(f => extOf(f.name) === 'lrc')
  if (audio.length) await addAudioFiles(audio)
  if (lyric.length) await addLyricFiles(lyric)
  if (!audio.length && !lyric.length) ElMessage.warning('没有识别到音频或 .lrc 文件')
}

const addAudioFiles = async (fileList) => {
  const list = Array.from(fileList).filter(f => AUDIO_EXT.includes(extOf(f.name)))
  if (!list.length) return
  parsing.value = true
  parseTotal.value = list.length
  const existing = new Set(rows.value.map(r => r.key))

  for (let i = 0; i < list.length; i++) {
    const f = list[i]
    parseLabel.value = `解析 ${f.name}`
    parseCurrent.value = i + 1
    const key = `${f.name}_${f.size}_${f.lastModified}`
    if (existing.has(key)) continue

    const id3 = await readId3FromFile(f)
    const byName = parseAudioFileName(f.name)
    const duration = await getAudioDuration(f).catch(() => null)

    rows.value.push({
      key,
      audio: f,
      title: (id3 && id3.title) || byName.title || baseNoExt(f.name),
      artist: (id3 && id3.artist) || byName.artist || '',
      album: (id3 && id3.album) || '',
      genre: (id3 && id3.genre) || '',
      duration,
      coverBlob: (id3 && id3.cover) || null,
      lyricText: '',
      lyricName: '',
      selected: true,
      status: 'idle',
      message: ''
    })
  }

  parsing.value = false
  parseCurrent.value = 0
  parseTotal.value = 0
  tryPairAll()
}

const addLyricFiles = async (fileList) => {
  const list = Array.from(fileList).filter(f => extOf(f.name) === 'lrc')
  if (!list.length) return
  parsing.value = true
  parseTotal.value = list.length
  parseLabel.value = '读取歌词'

  for (let i = 0; i < list.length; i++) {
    const f = list[i]
    parseCurrent.value = i + 1
    if (lyricPool.value.some(l => l.file.name === f.name && l.file.size === f.size)) continue
    const text = await readTextFile(f)
    const meta = parseLyricFileName(f.name)
    lyricPool.value.push({ file: f, text, title: meta.title, artist: meta.artist })
  }

  parsing.value = false
  parseCurrent.value = 0
  parseTotal.value = 0
  tryPairAll()
}

/** 给还没有歌词的行重新配对一次 */
const tryPairAll = () => {
  const usedFiles = new Set(rows.value.filter(r => r.lyricFile).map(r => r.lyricFile.name))
  rows.value.filter(r => !r.lyricText).forEach(row => {
    const available = lyricPool.value.filter(l => !usedFiles.has(l.file.name))

    // ① 同名文件（晴天.mp3 ↔ 晴天.lrc）
    let hit = available.find(l => norm(baseNoExt(l.file.name)) === norm(baseNoExt(row.audio.name)))
    // ② 歌名归一化相同
    if (!hit) hit = available.find(l => norm(l.title) === norm(row.title))
    // ③ 歌名 + 歌手都相同
    if (!hit && row.artist) hit = available.find(l => norm(l.title + l.artist) === norm(row.title + row.artist))
    // ④ 歌词命名反序（歌手-歌名-歌词）：把歌名与歌手对调再比一次
    if (!hit && row.artist) hit = available.find(l => norm(l.artist) === norm(row.title) && norm(l.title) === norm(row.artist))
    // ⑤ 包含关系兜底（如歌名 "青空" 对上 "青空Candy_Wind"），要求长度接近以免误配
    if (!hit) {
      const key = norm(row.title)
      hit = available.find(l => {
        const k = norm(l.title)
        if (!k || !key) return false
        const shorter = k.length <= key.length ? k : key
        const longer = k.length <= key.length ? key : k
        return shorter.length >= 2 && longer.includes(shorter) && longer.length - shorter.length <= 6
      })
    }

    if (hit) {
      row.lyricText = hit.text
      row.lyricName = hit.file.name
      row.lyricFile = hit.file
      usedFiles.add(hit.file.name)
    }
  })
}

// ==================== 手动补歌词 ====================
const pickLyricFor = (row) => {
  pendingLyricRow = row
  singleLyricPicker.value.click()
}
const onSingleLyricPicked = async (e) => {
  const f = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!f || !pendingLyricRow) return
  const text = await readTextFile(f)
  pendingLyricRow.lyricText = text
  pendingLyricRow.lyricName = f.name
  pendingLyricRow.lyricFile = f
  ElMessage.success('歌词已附加')
  pendingLyricRow = null
}

// ==================== 导入 ====================
const blobToFile = (blob, name) => new File([blob.data], name, { type: blob.mime || 'image/jpeg' })

const startImport = async () => {
  const list = rows.value.filter(r => r.selected && r.status !== 'done')
  if (!list.length) return
  importing.value = true
  importTotal.value = list.length
  importCurrent.value = 0
  let ok = 0
  let fail = 0

  for (let i = 0; i < list.length; i++) {
    const row = list[i]
    row.status = 'uploading'
    row.message = ''
    importLabel.value = row.title
    importCurrent.value = i

    try {
      // 1) 上传音频
      const audioRes = await uploadAudio(row.audio)
      const url = audioRes.data

      // 2) 有内嵌封面就一起上传（ID3 里的图）
      let coverUrl = ''
      if (row.coverBlob) {
        try {
          const coverRes = await uploadCover(blobToFile(row.coverBlob, `${row.key}_cover.jpg`))
          coverUrl = coverRes.data
        } catch (e) { /* 封面失败不影响导入 */ }
      }

      // 3) 创建歌曲
      await request.post('/admin/songs', {
        name: row.title,
        singer: row.artist,
        album: row.album,
        genre: row.genre,
        url,
        coverUrl,
        lyric: row.lyricText || null,
        duration: row.duration,
        format: extOf(row.audio.name),
        fileSize: row.audio.size,
        status: 1
      })
      row.status = 'done'
      row.message = '已导入'
      ok++
    } catch (e) {
      row.status = 'failed'
      row.message = e?.response?.data?.message || e?.message || '导入失败'
      fail++
    }
    importCurrent.value = i + 1
  }

  importing.value = false
  ElMessage.success(`导入完成：成功 ${ok} 首${fail ? `，失败 ${fail} 首` : ''}`)
}

const retryFailed = () => {
  const failed = rows.value.filter(r => r.status === 'failed')
  if (!failed.length) { ElMessage.info('没有失败的项'); return }
  failed.forEach(r => { r.status = 'idle'; r.selected = true })
  ElMessage.info(`已重置 ${failed.length} 首失败项，点「开始导入」重试`)
}

const toggleAll = (e) => {
  const v = e.target.checked
  rows.value.forEach(r => { if (r.status !== 'done') r.selected = v })
}

const resetAll = () => {
  rows.value = []
  lyricPool.value = []
}

onMounted(() => {})
</script>

<style scoped>
.batch-import { padding: 0 4px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 18px; gap: 16px; flex-wrap: wrap; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; }
.page-header .sub { font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 4px; }
.header-actions { display: flex; gap: 10px; flex-wrap: wrap; }

.drop-zone { border: 1.5px dashed rgba(102,126,234,0.45); border-radius: 10px; padding: 20px 16px; margin-bottom: 16px; text-align: center; color: rgba(255,255,255,0.55); background: rgba(102,126,234,0.05); transition: all 0.2s; }
.drop-zone.active { border-color: #667eea; background: rgba(102,126,234,0.18); }
.drop-title { margin: 6px 0 4px; font-size: 14px; color: rgba(255,255,255,0.8); }
.drop-hint { font-size: 12px; color: rgba(255,255,255,0.35); }

.bar-wrap { margin-bottom: 14px; }
.bar-label { font-size: 12px; color: rgba(255,255,255,0.55); margin-bottom: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bar { height: 6px; background: rgba(255,255,255,0.08); border-radius: 3px; overflow: hidden; }
.bar-inner { height: 100%; background: linear-gradient(90deg, #667eea, #764ba2); transition: width 0.2s; }
.bar-inner.blue { background: linear-gradient(90deg, #4facfe, #00f2fe); }

.stats { display: flex; gap: 18px; align-items: center; flex-wrap: wrap; font-size: 13px; color: rgba(255,255,255,0.5); margin-bottom: 12px; }
.stats b { color: rgba(255,255,255,0.85); }
.stats b.ok { color: #43e97b; }
.stats b.warn { color: #f5a623; }
.stats .tip { margin-left: auto; font-size: 12px; color: rgba(255,255,255,0.3); }

.data-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; }
.table-scroll { max-height: 52vh; overflow: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { position: sticky; top: 0; background: #1a1a2e; padding: 12px 10px; text-align: left; font-size: 12px; color: rgba(255,255,255,0.45); border-bottom: 1px solid rgba(255,255,255,0.08); z-index: 1; }
.data-table td { padding: 8px 10px; font-size: 13px; color: rgba(255,255,255,0.7); border-bottom: 1px solid rgba(255,255,255,0.04); }
.data-table tr.done td { background: rgba(67,233,123,0.05); }
.data-table tr.failed td { background: rgba(245,87,108,0.06); }
.data-table tr.uploading td { background: rgba(245,166,35,0.06); }
.cell-input { width: 100%; background: rgba(255,255,255,0.04); border: 1px solid transparent; border-radius: 4px; color: rgba(255,255,255,0.85); font-size: 13px; padding: 5px 7px; outline: none; }
.cell-input:focus { border-color: rgba(102,126,234,0.6); background: rgba(255,255,255,0.07); }
.td-sub { color: rgba(255,255,255,0.45); font-size: 12px; }
.ok { color: #43e97b; font-size: 12px; }

.empty { text-align: center; padding: 60px 20px; color: rgba(255,255,255,0.4); }
.empty .sub { font-size: 12px; color: rgba(255,255,255,0.3); margin-top: 8px; }
.empty code { background: rgba(255,255,255,0.08); padding: 2px 6px; border-radius: 4px; color: rgba(255,255,255,0.7); }

.footer { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-top: 16px; padding: 14px 16px; background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; }
.footer-left { flex: 1; min-width: 0; }
.footer-right { display: flex; gap: 10px; flex-shrink: 0; }
.tip { font-size: 12px; color: rgba(255,255,255,0.4); }

.btn { padding: 8px 14px; border: none; border-radius: 8px; font-size: 13px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; transition: all 0.2s; }
.btn-sm { padding: 3px 9px; font-size: 12px; }
.btn-primary { background: rgba(102,126,234,0.25); color: #a8b4ff; }
.btn-primary:hover:not(:disabled) { background: rgba(102,126,234,0.4); }
.btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-ghost { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.55); }
.btn-ghost:hover { background: rgba(255,255,255,0.12); }
</style>
