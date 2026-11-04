<template>
  <div class="song-manage">
    <div class="page-header">
      <h2 class="page-title">歌曲管理</h2>
    </div>

    <el-table :data="songs" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="封面" width="80">
        <template #default="{ row }">
          <el-image v-if="row.coverUrl" :src="row.coverUrl" style="width: 50px; height: 50px; border-radius: 4px" fit="cover" />
          <div v-else class="cover-placeholder-small">
            <el-icon :size="20"><Music /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="歌曲名" width="150" />
      <el-table-column prop="singer" label="歌手" width="120">
        <template #default="{ row }">
          <span class="singer-link" @click="$router.push(`/singer/${encodeURIComponent(row.singer)}`)">{{ row.singer }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="album" label="专辑" width="150">
        <template #default="{ row }">
          <span class="album-link" @click="$router.push(`/album/${encodeURIComponent(row.album)}`)">{{ row.album }}</span>
        </template>
      </el-table-column>
      <el-table-column label="音频" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.url" type="success" size="small">已上传</el-tag>
          <el-tag v-else type="danger" size="small">未上传</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="歌词" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.lyric" type="success" size="small">已上传</el-tag>
          <el-tag v-else type="info" size="small">未上传</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <div class="action-btn" @click="openEdit(row)">编辑</div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" title="编辑歌曲" width="500px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="歌曲名">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="歌手">
          <el-input v-model="editForm.singer" />
        </el-form-item>
        <el-form-item label="专辑">
          <el-input v-model="editForm.album" />
        </el-form-item>
        <el-form-item label="风格">
          <el-select v-model="editForm.genre" placeholder="选择歌曲风格" clearable filterable allow-create>
            <el-option v-for="g in genreOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="音频文件">
          <el-upload
            ref="audioUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".mp3,.wav,.flac,.aac,.ogg,.m4a"
            :on-change="handleAudioChange"
          >
            <el-button size="small" type="primary">选择音频文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 mp3/wav/flac/aac/ogg/m4a 格式</div>
            </template>
          </el-upload>
          <div v-if="editForm.url" class="file-hint">当前音频: {{ getFileName(editForm.url) }}</div>
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
            ref="coverUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".jpg,.jpeg,.png,.gif,.webp"
            :on-change="handleCoverChange"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div v-if="editForm.coverUrl" class="file-hint">当前封面: {{ getFileName(editForm.coverUrl) }}</div>
        </el-form-item>
        <el-form-item label="歌词文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".lrc"
            :on-change="handleLyricChange"
          >
            <el-button size="small" type="primary">选择歌词文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持 .lrc 格式</div>
            </template>
          </el-upload>
          <div v-if="editForm.lyric" class="file-hint">当前歌词: {{ getFileName(editForm.lyric) }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllSongs, uploadAudio, uploadCover, updateSong, getAudioDuration } from '@/api/song'
import { ElMessage } from 'element-plus'

// 读文本文件：.lrc 在中文 Windows 上常见 GBK 编码，UTF-8 解出乱码时退回 GBK
const readTextFile = async (file) => {
  const buf = await file.arrayBuffer()
  let text = new TextDecoder('utf-8').decode(buf)
  if (text.includes('\uFFFD')) {
    try { text = new TextDecoder('gbk').decode(buf) } catch (e) { /* 浏览器不支持则保持原样 */ }
  }
  return text.replace(/^\uFEFF/, '')
}

const songs = ref([])
const editVisible = ref(false)
const saving = ref(false)
const audioFile = ref(null)
const coverFile = ref(null)
const lyricFile = ref(null)

const editForm = ref({
  id: null,
  name: '',
  singer: '',
  album: '',
  genre: '',
  url: '',
  coverUrl: '',
  lyric: ''
})

const genreOptions = ['流行', '摇滚', '民谣', '电子', '古典', '爵士', 'R&B', '嘻哈', '金属', '乡村', '蓝调', '雷鬼', '拉丁', '新世纪', '中国风', '古风', '轻音乐', '纯音乐', '原声带', '其他']

onMounted(async () => {
  const res = await getAllSongs()
  songs.value = res.data || []
})

const openEdit = (row) => {
  editForm.value = { ...row }
  audioFile.value = null
  coverFile.value = null
  editVisible.value = true
}

const handleAudioChange = (file) => {
  audioFile.value = file.raw
}

const handleCoverChange = (file) => {
  coverFile.value = file.raw
}

const handleLyricChange = (file) => {
  lyricFile.value = file.raw
}

const getFileName = (url) => {
  if (!url) return ''
  return url.split('/').pop()
}

const handleSave = async () => {
  saving.value = true
  try {
    if (audioFile.value) {
      const audioRes = await uploadAudio(audioFile.value)
      editForm.value.url = audioRes.data
      const duration = await getAudioDuration(audioFile.value)
      if (duration) editForm.value.duration = duration
    }
    if (coverFile.value) {
      const coverRes = await uploadCover(coverFile.value)
      editForm.value.coverUrl = coverRes.data
    }
    if (lyricFile.value) {
      // 直接读文件内容存进 lyric 字段（原来存的是上传后的 URL，前端解析不出歌词）
      editForm.value.lyric = await readTextFile(lyricFile.value)
    }

    const { id, name, singer, album, genre, url, coverUrl, lyric, duration } = editForm.value
    await updateSong(id, { name, singer, album, genre, url, coverUrl, lyric, duration })

    ElMessage.success('保存成功')
    editVisible.value = false

    const res = await getAllSongs()
    songs.value = res.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.song-manage {
  padding: 20px 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  color: var(--text-primary);
}

.cover-placeholder-small {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.file-hint {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.singer-link {
  color: var(--accent-cyan);
  cursor: pointer;
}

.singer-link:hover {
  text-decoration: underline;
}

.album-link {
  color: var(--accent-amber);
  cursor: pointer;
}

.album-link:hover {
  text-decoration: underline;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.action-btn:hover {
  color: var(--primary-color);
}
</style>
