<template>
  <div class="album-detail-page">
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="songs.length">
      <div class="album-header">
        <div class="album-cover">
          <img v-if="songs[0].coverUrl" :src="songs[0].coverUrl" />
          <div v-else class="cover-placeholder">
            <el-icon :size="60"><Music /></el-icon>
          </div>
        </div>
        <div class="album-info">
          <h1 class="album-name">{{ albumName }}</h1>
          <div class="album-stats">
            <span class="stat-item">
              <el-icon><Music /></el-icon>
              歌曲 {{ songs.length }} 首
            </span>
          </div>
        </div>
      </div>

      <div class="album-songs">
        <h2 class="section-title">歌曲列表</h2>
        <el-table :data="paginatedSongs" stripe style="width: 100%" @row-click="playSong">
          <el-table-column type="index" label="#" width="60" :index="indexMethod" />
          <el-table-column prop="name" label="歌曲" width="300">
            <template #default="{ row }">
              <span class="song-name-link" @click.stop="$router.push(`/song/${row.id}`)">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="singer" label="歌手" width="200">
            <template #default="{ row }">
              <span class="singer-link" @click.stop="$router.push(`/singer/${encodeURIComponent(row.singer)}`)">{{ row.singer }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="时长" width="100">
            <template #default="{ row }">
              {{ formatDuration(row.duration) }}
            </template>
          </el-table-column>
          <el-table-column prop="playCount" label="播放次数" width="120" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <div class="action-btn" @click.stop="playSong(row)">
                <el-icon><VideoPlay /></el-icon> 播放
              </div>
              <div v-if="userStore.isLoggedIn()" class="action-btn" @click.stop="handleAddToQueue(row)" title="添加到播放列表">
                <el-icon><CirclePlus /></el-icon>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            :current-page="currentPage"
            @update:current-page="currentPage = $event"
            :page-size="pageSize"
            :total="songs.length"
            layout="prev, pager, next"
            background
            small
          />
        </div>
      </div>
    </template>

    <el-empty v-else description="暂无歌曲" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { fillMissingDurations } from '@/api/song'

const route = useRoute()
const musicStore = useMusicStore()
const userStore = useUserStore()

const loading = ref(true)
const songs = ref([])
const currentPage = ref(1)
const pageSize = 12

const paginatedSongs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return songs.value.slice(start, start + pageSize)
})

const indexMethod = (index) => {
  return (currentPage.value - 1) * pageSize + index + 1
}

const albumName = computed(() => decodeURIComponent(route.params.name))

onMounted(async () => {
  try {
    const res = await request.get(`/albums/${encodeURIComponent(albumName.value)}`)
    songs.value = res.data || []
    fillMissingDurations(songs.value)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

const playSong = (song) => {
  musicStore.setCurrentSong(song)
}

const handleAddToQueue = (song) => {
  const added = musicStore.addToQueue(song)
  if (added) {
    ElMessage.success('已添加到播放列表')
  } else {
    ElMessage.info('歌曲已在播放列表中')
  }
}

const formatDuration = (seconds) => {
  if (!seconds) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.album-detail-page {
  padding: 20px;
  min-height: calc(100vh - 140px);
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
  gap: 16px;
  color: var(--text-secondary);
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.album-header {
  display: flex;
  gap: 30px;
  padding: 30px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border-radius: 12px;
  margin-bottom: 30px;
  color: #fff;
}

.album-cover {
  flex-shrink: 0;
}

.album-cover img {
  width: 150px;
  height: 150px;
  border-radius: 12px;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.3);
}

.cover-placeholder {
  width: 150px;
  height: 150px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4px solid rgba(255, 255, 255, 0.3);
}

.album-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.album-name {
  font-size: 32px;
  margin: 0 0 16px 0;
}

.album-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  opacity: 0.9;
}

.album-songs {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 20px;
}

.section-title {
  font-size: 20px;
  margin: 0 0 20px 0;
  color: var(--text-primary);
}

.song-name-link {
  color: var(--primary-color);
  cursor: pointer;
}

.song-name-link:hover {
  text-decoration: underline;
}

.singer-link {
  color: var(--accent-cyan);
  cursor: pointer;
}

.singer-link:hover {
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

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
