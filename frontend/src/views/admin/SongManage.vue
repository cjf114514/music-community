<template>
  <div class="song-manage">
    <div class="page-header">
      <h2>歌曲管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="openAddDialog">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
          新增歌曲
        </button>
        <div class="search-box">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
          <input v-model="searchForm.keyword" placeholder="搜索歌曲名或歌手" @keyup.enter="loadSongs" />
        </div>
        <select v-model="searchForm.status" class="filter-select" @change="loadSongs">
          <option :value="null">全部状态</option>
          <option :value="1">正常</option>
          <option :value="0">下架</option>
        </select>
      </div>
    </div>

    <div class="data-card">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:50px"></th>
            <th>歌曲信息</th>
            <th>歌手</th>
            <th>专辑</th>
            <th>播放量</th>
            <th>状态</th>
            <th>添加时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="song in songs" :key="song.id">
            <td>
              <div class="song-cover" v-if="song.coverUrl">
                <img :src="song.coverUrl" alt="" />
              </div>
              <div class="song-cover placeholder" v-else>
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
              </div>
            </td>
            <td>
              <div class="song-name-cell">
                <span class="song-name">{{ song.name }}</span>
                <span class="song-format" v-if="song.format">{{ song.format }}</span>
              </div>
            </td>
            <td>{{ song.singer || '-' }}</td>
            <td class="td-sub">{{ song.album || '-' }}</td>
            <td>
              <span class="play-count">{{ formatCount(song.playCount) }}</span>
            </td>
            <td>
              <span class="status-badge" :class="song.status === 1 ? 'active' : 'disabled'">
                {{ song.status === 1 ? '正常' : '下架' }}
              </span>
            </td>
            <td class="td-time">{{ formatTime(song.createTime) }}</td>
            <td>
              <div class="action-btns">
                <button class="btn btn-sm btn-primary" @click="openEditDialog(song)">编辑</button>
                <button class="btn btn-sm" :class="song.status === 1 ? 'btn-danger' : 'btn-success'" @click="toggleStatus(song)">
                  {{ song.status === 1 ? '下架' : '上架' }}
                </button>
                <button class="btn btn-sm btn-danger-outline" @click="deleteSong(song)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span class="total-text">共 {{ pagination.total }} 条</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="pagination.page <= 0" @click="pagination.page--; loadSongs()">上一页</button>
          <span class="page-info">{{ pagination.page + 1 }} / {{ Math.ceil(pagination.total / pagination.size) || 1 }}</span>
          <button class="page-btn" :disabled="(pagination.page + 1) * pagination.size >= pagination.total" @click="pagination.page++; loadSongs()">下一页</button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" class="admin-dialog" :append-to-body="true">
      <el-form :model="form" label-width="80px" class="dark-form">
        <!-- ===== 一体化拖拽区：音频 + 歌词 + 封面 一次拖进来 ===== -->
        <div
          class="drop-zone"
          :class="{ active: dragOver }"
          @dragover.prevent="dragOver = true"
          @dragleave.prevent="dragOver = false"
          @drop.prevent="handleDrop"
          @click="openFilePicker"
        >
          <el-icon :size="24"><UploadFilled /></el-icon>
          <p class="drop-title">把文件拖到这里，或点击选择</p>
          <p class="drop-hint">音频 .mp3 .flac .wav .m4a　+　歌词 .lrc　+　封面图，可以一次全拖进来</p>
          <p class="drop-hint">文件名写成「歌手 - 歌名.mp3」会自动填好歌手和歌名</p>
          <div v-if="fileStatus.length" class="file-list" @click.stop>
            <div v-for="(f, i) in fileStatus" :key="i" class="file-item" :class="f.state">
              <span class="file-name">{{ f.name }}</span>
              <span class="file-msg">{{ f.msg }}</span>
            </div>
          </div>
        </div>
        <input
          ref="filePicker"
          type="file"
          multiple
          accept=".mp3,.wav,.flac,.aac,.ogg,.m4a,.lrc,.jpg,.jpeg,.png,.gif,.webp"
          style="display: none"
          @change="handlePicked"
        />
        <el-form-item label="歌曲名" required>
          <el-input v-model="form.name" placeholder="请输入歌曲名" />
        </el-form-item>
        <el-form-item label="歌手" required>
          <el-input v-model="form.singer" placeholder="请输入歌手名" />
        </el-form-item>
        <el-form-item label="专辑">
          <el-input v-model="form.album" placeholder="请输入专辑名" />
        </el-form-item>
        <el-form-item label="风格">
          <el-select v-model="form.genre" placeholder="选择歌曲风格" clearable filterable allow-create>
            <el-option v-for="g in genreOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="音频文件">
          <div class="upload-row" @click.stop>
            <el-upload
              ref="audioUploadRef"
              :auto-upload="false"
              :limit="1"
              accept=".mp3,.wav,.flac,.aac,.ogg,.m4a"
              :on-change="handleAudioChange"
              :show-file-list="true"
            >
              <button class="btn btn-primary btn-sm">选择音频</button>
            </el-upload>
          </div>
          <el-input v-model="form.url" placeholder="或手动输入音频URL" @click.stop @focus="(e)=>e.target.select()" />
          <div v-if="form.url && !form.url.startsWith('http')" class="upload-status uploading">上传中...</div>
          <div v-else-if="form.url && form.url.startsWith('http')" class="upload-status success">✓ 已获取URL</div>
        </el-form-item>
        <el-form-item label="封面图片">
          <div class="upload-row" @click.stop>
            <el-upload
              ref="coverUploadRef"
              :auto-upload="false"
              :limit="1"
              accept=".jpg,.jpeg,.png,.gif,.webp"
              :on-change="handleCoverChange"
              list-type="picture-card"
              :show-file-list="false"
            >
              <img v-if="form.coverUrl && form.coverUrl.startsWith('http')" :src="form.coverUrl" style="width:100%;height:100%;object-fit:cover" />
              <el-icon v-else :size="24"><Plus /></el-icon>
            </el-upload>
          </div>
          <el-input v-model="form.coverUrl" placeholder="或手动输入封面URL" @click.stop />
        </el-form-item>
        <el-form-item label="歌词">
          <div class="upload-row" @click.stop>
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".lrc"
              :on-change="handleLyricChange"
            >
              <button class="btn btn-sm">选择LRC文件</button>
            </el-upload>
          </div>
          <el-input v-model="form.lyric" type="textarea" :rows="6" placeholder="或粘贴LRC格式歌词" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="btn btn-ghost" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" @click="saveSong">确定</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UploadFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { uploadAudio, uploadCover, getAudioDuration } from '@/api/song'

const songs = ref([])
const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ page: 0, size: 20, total: 0 })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive({ id: null, name: '', singer: '', album: '', genre: '', url: '', coverUrl: '', lyric: '' })

const genreOptions = ['流行', '摇滚', '民谣', '电子', '古典', '爵士', 'R&B', '嘻哈', '金属', '乡村', '蓝调', '雷鬼', '拉丁', '新世纪', '中国风', '古风', '轻音乐', '纯音乐', '原声带', '其他']

// ==================== 一体化拖拽导入 ====================
const dragOver = ref(false)
const fileStatus = ref([])
const filePicker = ref(null)

const AUDIO_EXT = ['mp3', 'wav', 'flac', 'aac', 'ogg', 'm4a']
const IMAGE_EXT = ['jpg', 'jpeg', 'png', 'gif', 'webp']

const extOf = (name) => (name.toLowerCase().split('.').pop() || '')

const classifyFile = (file) => {
  const ext = extOf(file.name)
  if (AUDIO_EXT.includes(ext)) return 'audio'
  if (ext === 'lrc') return 'lyric'
  if (IMAGE_EXT.includes(ext)) return 'cover'
  return 'unknown'
}

/**
 * 从「歌手 - 歌名.mp3」里拆出歌手和歌名
 * 支持：歌手 - 歌名 / 歌手-歌名 / 01. 歌手 - 歌名
 * 拆不出来就整体当歌名（用户可以在表单里改）
 */
const parseFileName = (fileName) => {
  let base = fileName.replace(/\.[^.]+$/, '')
  base = base.replace(/^\s*\d{1,3}\s*[.\-_、)]\s*/, '')       // 去掉音轨号 "01." "02_"
  const m = base.match(/^(.+?)\s*[-–—－]\s*(.+)$/)
  if (m) return { singer: m[1].trim(), name: m[2].trim() }
  return { singer: '', name: base.trim() }
}

/**
 * 读文本文件
 * .lrc 在中文 Windows 上常见 GBK 编码，先按 UTF-8 读，
 * 出现替换字符（\uFFFD）说明解错了，再退回 GBK
 */
const readTextFile = async (file) => {
  const buf = await file.arrayBuffer()
  let text = new TextDecoder('utf-8').decode(buf)
  if (text.includes('\uFFFD')) {
    try { text = new TextDecoder('gbk').decode(buf) } catch (e) { /* 浏览器不支持 GBK 就保持原样 */ }
  }
  return text.replace(/^\uFEFF/, '')
}

const setStatus = (name, state, msg) => {
  const exist = fileStatus.value.find(f => f.name === name)
  if (exist) { exist.state = state; exist.msg = msg } else { fileStatus.value.push({ name, state, msg }) }
}

const applyAudioFile = async (file) => {
  const parsed = parseFileName(file.name)
  if (!form.name) form.name = parsed.name
  if (!form.singer && parsed.singer) form.singer = parsed.singer
  setStatus(file.name, 'uploading', '上传中...')
  form.url = 'uploading...'
  try {
    const res = await uploadAudio(file)
    form.url = res.data
    const duration = await getAudioDuration(file)
    if (duration) form.duration = duration
    form.format = extOf(file.name)
    form.fileSize = file.size
    const tip = duration ? `已上传（${Math.floor(duration / 60)}:${String(duration % 60).padStart(2, '0')}）` : '已上传'
    const parsedTip = parsed.singer ? `，已自动填入「${parsed.singer} / ${parsed.name}」` : ''
    setStatus(file.name, 'done', tip + parsedTip)
  } catch (e) {
    form.url = ''
    setStatus(file.name, 'failed', '上传失败')
    ElMessage.error(`音频上传失败：${file.name}`)
  }
}

// 歌词：直接读文件内容存进表单，不再上传文件（修复原来存 URL 导致歌词解析为空的问题）
const applyLyricFile = async (file) => {
  try {
    const text = await readTextFile(file)
    const lineCount = (text.match(/\[\d{2}:\d{2}/g) || []).length
    form.lyric = text
    setStatus(file.name, 'done', lineCount ? `已读取 ${lineCount} 行歌词` : '已读取（未检测到时间标签，请检查格式）')
  } catch (e) {
    setStatus(file.name, 'failed', '读取失败')
  }
}

const applyCoverFile = async (file) => {
  setStatus(file.name, 'uploading', '上传中...')
  try {
    const res = await uploadCover(file)
    form.coverUrl = res.data
    setStatus(file.name, 'done', '已设为封面')
  } catch (e) {
    setStatus(file.name, 'failed', '上传失败')
  }
}

const processFiles = async (files) => {
  fileStatus.value = []
  const list = Array.from(files || [])
  if (!list.length) return

  const audioCount = list.filter(f => classifyFile(f) === 'audio').length
  if (audioCount > 1) ElMessage.warning('一次只能导入一首，多余的音频已忽略')

  let audioUsed = false
  for (const file of list) {
    const kind = classifyFile(file)
    if (kind === 'audio') {
      if (audioUsed) { setStatus(file.name, 'skipped', '已忽略（一次一首）'); continue }
      audioUsed = true
      await applyAudioFile(file)
    } else if (kind === 'lyric') {
      await applyLyricFile(file)
    } else if (kind === 'cover') {
      await applyCoverFile(file)
    } else {
      setStatus(file.name, 'skipped', '不支持的格式')
    }
  }
}

const handleDrop = async (e) => {
  dragOver.value = false
  await processFiles(e.dataTransfer?.files)
}

const openFilePicker = () => filePicker.value && filePicker.value.click()

const handlePicked = async (e) => {
  await processFiles(e.target.files)
  e.target.value = ''   // 清空，允许再次选同一个文件
}

const formatCount = (n) => {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

const loadSongs = async () => {
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    const res = await request.get('/admin/songs', { params })
    if (res.code === 200) {
      songs.value = res.data.list
      pagination.total = res.data.total
    }
  } catch (e) { ElMessage.error('加载失败') }
}

const openAddDialog = () => {
  dialogTitle.value = '新增歌曲'
  Object.assign(form, { id: null, name: '', singer: '', album: '', genre: '', url: '', coverUrl: '', lyric: '', duration: null, fileSize: null, format: null })
  fileStatus.value = []
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogTitle.value = '编辑歌曲'
  Object.assign(form, { ...row })
  fileStatus.value = []
  dialogVisible.value = true
}

const handleAudioChange = async (uploadFile) => {
  try {
    form.url = 'uploading...'
    const res = await uploadAudio(uploadFile.raw)
    form.url = res.data
    const duration = await getAudioDuration(uploadFile.raw)
    if (duration) form.duration = duration
    ElMessage.success('音频上传成功')
  } catch (e) {
    form.url = ''
    ElMessage.error('音频上传失败')
  }
}

const handleCoverChange = async (uploadFile) => {
  try {
    const res = await uploadCover(uploadFile.raw)
    form.coverUrl = res.data
    ElMessage.success('封面上传成功')
  } catch (e) {
    ElMessage.error('封面上传失败')
  }
}

// 手动「选择LRC文件」按钮：同样直接读内容，不占用服务器存储
const handleLyricChange = async (uploadFile) => {
  try {
    form.lyric = await readTextFile(uploadFile.raw)
    ElMessage.success('歌词已读取')
  } catch (e) {
    ElMessage.error('歌词读取失败')
  }
}

const saveSong = async () => {
  try {
    if (form.id) {
      await request.put(`/admin/songs/${form.id}`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/admin/songs', form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadSongs()
  } catch (e) { ElMessage.error('操作失败') }
}

const toggleStatus = async (row) => {
  try {
    await request.put(`/admin/songs/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success('操作成功')
    loadSongs()
  } catch (e) { ElMessage.error('操作失败') }
}

const deleteSong = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除此歌曲吗？', '警告', { type: 'warning' })
    await request.delete(`/admin/songs/${row.id}`)
    ElMessage.success('删除成功')
    loadSongs()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => loadSongs())
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 8px 12px; color: rgba(255,255,255,0.4); }
.search-box input { background: none; border: none; outline: none; color: #fff; font-size: 13px; width: 180px; }
.search-box input::placeholder { color: rgba(255,255,255,0.3); }
.filter-select { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 8px 12px; color: #fff; font-size: 13px; outline: none; }
.filter-select option { background: #1a1a2e; }
.data-card { background: rgba(26, 26, 46, 0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { padding: 14px 16px; text-align: left; font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.4); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.data-table td { padding: 12px 16px; font-size: 13px; color: rgba(255,255,255,0.7); border-bottom: 1px solid rgba(255,255,255,0.04); }
.data-table tr:hover td { background: rgba(255,255,255,0.02); }
.td-sub { color: rgba(255,255,255,0.4); }
.td-time { color: rgba(255,255,255,0.4); font-size: 12px; }
.song-cover { width: 44px; height: 44px; border-radius: 8px; overflow: hidden; }
.song-cover img { width: 100%; height: 100%; object-fit: cover; }
.song-cover.placeholder { background: rgba(102,126,234,0.15); display: flex; align-items: center; justify-content: center; color: #667eea; }
.song-name-cell { display: flex; flex-direction: column; gap: 2px; }
.song-name { color: rgba(255,255,255,0.85); font-weight: 500; }
.song-format { font-size: 11px; color: rgba(255,255,255,0.3); }
.play-count { color: rgba(255,255,255,0.5); font-size: 13px; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; }
.status-badge.active { background: rgba(67,233,123,0.15); color: #43e97b; }
.status-badge.disabled { background: rgba(245,87,108,0.15); color: #f5576c; }
.action-btns { display: flex; gap: 6px; }
.btn { padding: 6px 12px; border: none; border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.2s; white-space: nowrap; display: inline-flex; align-items: center; gap: 6px; }
.btn-sm { padding: 4px 10px; }
.btn-primary { background: rgba(102,126,234,0.2); color: #667eea; }
.btn-primary:hover { background: rgba(102,126,234,0.3); }
.btn-danger { background: rgba(245,87,108,0.15); color: #f5576c; }
.btn-danger:hover { background: rgba(245,87,108,0.25); }
.btn-danger-outline { background: transparent; color: #f5576c; border: 1px solid rgba(245,87,108,0.3); }
.btn-danger-outline:hover { background: rgba(245,87,108,0.1); }
.btn-success { background: rgba(67,233,123,0.15); color: #43e97b; }
.btn-success:hover { background: rgba(67,233,123,0.25); }
.btn-ghost { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.5); }
.btn-ghost:hover { background: rgba(255,255,255,0.1); }
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 16px; }
.total-text { font-size: 13px; color: rgba(255,255,255,0.4); }
.page-btns { display: flex; align-items: center; gap: 12px; }
.page-btn { padding: 6px 14px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; color: rgba(255,255,255,0.6); font-size: 13px; cursor: pointer; }
.page-btn:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 13px; color: rgba(255,255,255,0.5); }
/* ===== 一体化拖拽区 ===== */
.drop-zone { border: 1.5px dashed rgba(102,126,234,0.45); border-radius: 10px; padding: 18px 16px; margin-bottom: 18px; text-align: center; color: rgba(255,255,255,0.55); cursor: pointer; transition: all 0.2s; background: rgba(102,126,234,0.05); }
.drop-zone:hover { border-color: rgba(102,126,234,0.75); background: rgba(102,126,234,0.1); }
.drop-zone.active { border-color: #667eea; background: rgba(102,126,234,0.18); }
.drop-title { margin: 6px 0 4px; font-size: 14px; color: rgba(255,255,255,0.8); font-weight: 500; }
.drop-hint { margin: 2px 0; font-size: 12px; color: rgba(255,255,255,0.35); line-height: 1.5; }
.file-list { margin-top: 12px; text-align: left; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 10px; }
.file-item { display: flex; justify-content: space-between; gap: 12px; font-size: 12px; padding: 4px 6px; border-radius: 4px; }
.file-item .file-name { color: rgba(255,255,255,0.7); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 60%; }
.file-item .file-msg { color: rgba(255,255,255,0.4); flex-shrink: 0; }
.file-item.done .file-msg { color: #43e97b; }
.file-item.failed .file-msg { color: #f5576c; }
.file-item.uploading .file-msg { color: #f5a623; }
.file-item.skipped .file-msg { color: rgba(255,255,255,0.3); }
.upload-row { margin-bottom: 8px; }
.upload-status { font-size: 12px; margin-top: 4px; }
.upload-status.uploading { color: #f5a623; }
.upload-status.success { color: #43e97b; }
</style>
