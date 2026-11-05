<template>
  <div class="smart-page">
    <div class="page-header">
      <div>
        <h2>智能歌单</h2>
        <p class="sub">按规则自动筛选歌曲，不用手动往歌单里加歌</p>
      </div>
      <button class="btn btn-primary" :disabled="!songs.length" @click="playAll">
        <el-icon><VideoPlay /></el-icon> 播放全部（{{ songs.length }}）
      </button>
    </div>

    <!-- 快捷规则 -->
    <div class="presets">
      <span class="label">快捷</span>
      <button
        v-for="p in presets" :key="p.key"
        class="preset" :class="{ active: activePreset === p.key }"
        @click="applyPreset(p)"
      >{{ p.label }}</button>
    </div>

    <!-- 高级条件 -->
    <div class="filters">
      <div class="filter">
        <span class="label">流派</span>
        <el-select v-model="form.genre" placeholder="不限" clearable filterable allow-create size="small" style="width: 140px">
          <el-option v-for="g in genreOptions" :key="g" :label="g" :value="g" />
        </el-select>
      </div>
      <div class="filter">
        <span class="label">歌手包含</span>
        <el-input v-model="form.singer" placeholder="不限" clearable size="small" style="width: 130px" @keyup.enter="load" />
      </div>
      <div class="filter">
        <span class="label">播放次数</span>
        <el-input-number v-model="form.minPlay" :min="0" :controls="false" placeholder="最少" size="small" style="width: 78px" />
        <span class="sep">~</span>
        <el-input-number v-model="form.maxPlay" :min="0" :controls="false" placeholder="最多" size="small" style="width: 78px" />
      </div>
      <div class="filter">
        <span class="label">添加于最近</span>
        <el-input-number v-model="form.addedWithinDays" :min="0" :controls="false" placeholder="天" size="small" style="width: 78px" />
        <span class="unit">天</span>
      </div>
      <div class="filter">
        <span class="label">排序</span>
        <el-select v-model="form.order" size="small" style="width: 130px">
          <el-option label="最新添加" value="newest" />
          <el-option label="最早添加" value="oldest" />
          <el-option label="播放最多" value="playCountDesc" />
          <el-option label="播放最少" value="playCountAsc" />
        </el-select>
      </div>
      <div class="filter">
        <span class="label">数量</span>
        <el-select v-model="form.limit" size="small" style="width: 90px">
          <el-option v-for="n in [20, 50, 100, 200, 500]" :key="n" :label="n + ' 首'" :value="n" />
        </el-select>
      </div>
      <button class="btn btn-primary btn-sm" @click="load">筛选</button>
      <button class="btn btn-ghost btn-sm" @click="reset">重置</button>
    </div>

    <!-- 结果 -->
    <div v-if="loading" class="empty">筛选中…</div>
    <div v-else-if="!songs.length" class="empty">
      <p>没有符合条件的歌曲</p>
      <p class="sub">试着放宽条件，或者先把音乐导入进来</p>
    </div>
    <div v-else class="data-card">
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 56px">#</th>
              <th>歌名</th>
              <th style="width: 200px">歌手</th>
              <th style="width: 160px">流派</th>
              <th style="width: 90px">播放</th>
              <th style="width: 110px">添加时间</th>
              <th style="width: 70px"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(s, i) in songs" :key="s.id" @dblclick="playOne(s)">
              <td class="idx">{{ i + 1 }}</td>
              <td class="name">{{ s.name }}</td>
              <td class="sub">{{ s.singer || '—' }}</td>
              <td class="sub">{{ s.genre || '—' }}</td>
              <td class="sub">{{ s.playCount || 0 }}</td>
              <td class="sub">{{ fmtDate(s.createTime) }}</td>
              <td><button class="play-btn" @click.stop="playOne(s)">▶</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useMusicStore } from '@/stores/music'

const store = useMusicStore()

const songs = ref([])
const loading = ref(false)
const activePreset = ref('')

const form = ref({
  genre: '',
  singer: '',
  minPlay: null,
  maxPlay: null,
  addedWithinDays: null,
  order: 'newest',
  limit: 100
})

const genreOptions = ['流行', '摇滚', '民谣', '电子', '说唱', '爵士', '古典', '纯音乐', '轻音乐', '古风', 'ACG', '原声带', '其他']

const presets = [
  { key: 'never', label: '从未听过', patch: { maxPlay: 0, minPlay: null, order: 'newest' } },
  { key: 'top', label: '最常听', patch: { minPlay: 1, maxPlay: null, order: 'playCountDesc' } },
  { key: 'week', label: '最近一周添加', patch: { addedWithinDays: 7, order: 'newest' } },
  { key: 'cold', label: '很久没听', patch: { minPlay: 1, maxPlay: null, order: 'playCountAsc' } },
  { key: 'all', label: '全部', patch: { order: 'newest' } }
]

const fmtDate = (t) => (t ? String(t).replace('T', ' ').slice(0, 10) : '—')

const applyPreset = (p) => {
  activePreset.value = p.key
  form.value = {
    genre: '', singer: '', minPlay: null, maxPlay: null, addedWithinDays: null,
    order: 'newest', limit: 100, ...p.patch
  }
  load()
}

const load = async () => {
  loading.value = true
  try {
    const params = { order: form.value.order, limit: form.value.limit }
    if (form.value.genre) params.genre = form.value.genre
    if (form.value.singer) params.singer = form.value.singer
    if (form.value.minPlay !== null && form.value.minPlay !== undefined && form.value.minPlay !== '') params.minPlay = form.value.minPlay
    if (form.value.maxPlay !== null && form.value.maxPlay !== undefined && form.value.maxPlay !== '') params.maxPlay = form.value.maxPlay
    if (form.value.addedWithinDays) params.addedWithinDays = form.value.addedWithinDays

    const res = await request.get('/songs/smart', { params })
    songs.value = res.data || []
  } catch (e) {
    ElMessage.error('筛选失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  activePreset.value = ''
  form.value = { genre: '', singer: '', minPlay: null, maxPlay: null, addedWithinDays: null, order: 'newest', limit: 100 }
  load()
}

const playAll = () => {
  if (!songs.value.length) return
  store.setPlayList([...songs.value])
  store.setCurrentSong(songs.value[0])
  ElMessage.success(`开始播放 ${songs.value.length} 首`)
}

const playOne = (song) => {
  const idx = songs.value.findIndex(s => s.id === song.id)
  store.setPlayList([...songs.value])
  store.setCurrentSong(songs.value[idx] || song)
}

onMounted(() => {
  applyPreset(presets[0])
})
</script>

<style scoped>
.smart-page { padding: 24px 32px 120px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; flex-wrap: wrap; }
.page-header h2 { font-size: 22px; font-weight: 600; color: #fff; }
.page-header .sub { font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 4px; }

.presets { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
.presets .label { font-size: 12px; color: rgba(255,255,255,0.4); }
.preset { padding: 5px 12px; border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.6); border-radius: 14px; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.preset:hover { background: rgba(255,255,255,0.1); }
.preset.active { background: rgba(102,126,234,0.25); border-color: rgba(102,126,234,0.5); color: #a8b4ff; }

.filters { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; padding: 14px 16px; margin-bottom: 16px; background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; }
.filter { display: flex; align-items: center; gap: 6px; }
.filter .label { font-size: 12px; color: rgba(255,255,255,0.45); white-space: nowrap; }
.filter .sep, .filter .unit { font-size: 12px; color: rgba(255,255,255,0.3); }

.btn { padding: 8px 16px; border: none; border-radius: 8px; font-size: 13px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
.btn-sm { padding: 6px 12px; font-size: 12px; }
.btn-primary { background: rgba(102,126,234,0.25); color: #a8b4ff; }
.btn-primary:hover:not(:disabled) { background: rgba(102,126,234,0.42); }
.btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-ghost { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.55); }
.btn-ghost:hover { background: rgba(255,255,255,0.12); }

.empty { text-align: center; padding: 70px 20px; color: rgba(255,255,255,0.45); }
.empty .sub { font-size: 12px; color: rgba(255,255,255,0.3); margin-top: 8px; }

.data-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; }
.table-scroll { max-height: 62vh; overflow: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { position: sticky; top: 0; background: #1a1a2e; padding: 12px 14px; text-align: left; font-size: 12px; color: rgba(255,255,255,0.45); border-bottom: 1px solid rgba(255,255,255,0.08); z-index: 1; }
.data-table td { padding: 10px 14px; font-size: 13px; color: rgba(255,255,255,0.7); border-bottom: 1px solid rgba(255,255,255,0.04); }
.data-table tr:hover td { background: rgba(255,255,255,0.03); }
.idx { color: rgba(255,255,255,0.3); font-size: 12px; }
.name { color: rgba(255,255,255,0.88); }
.sub { color: rgba(255,255,255,0.45); font-size: 12px; }
.play-btn { width: 26px; height: 26px; border-radius: 50%; border: none; background: rgba(102,126,234,0.2); color: #a8b4ff; cursor: pointer; font-size: 11px; }
.play-btn:hover { background: rgba(102,126,234,0.45); }
</style>
