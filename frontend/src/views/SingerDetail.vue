<template>
  <div class="singer-detail-page">
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="singer.name">
      <div class="singer-header">
        <div class="singer-avatar">
          <img v-if="singer.avatar" :src="singer.avatar" />
          <div v-else class="avatar-placeholder">
            <el-icon :size="60"><UserFilled /></el-icon>
          </div>
        </div>
        <div class="singer-info">
          <h1 class="singer-name">{{ singer.name }}</h1>
          <div class="singer-stats">
            <span class="stat-item">
              <el-icon><Music /></el-icon>
              歌曲 {{ singer.songCount || 0 }} 首
            </span>
          </div>
          <p v-if="singer.description" class="singer-desc">{{ singer.description }}</p>
          <p v-else class="singer-desc placeholder">暂无简介</p>
        </div>
      </div>

      <div class="singer-songs">
        <h2 class="section-title">歌曲列表</h2>
        <el-table :data="paginatedSongs" stripe style="width: 100%" @row-click="playSong">
          <el-table-column type="index" label="#" width="60" :index="indexMethod" />
          <el-table-column prop="name" label="歌曲" width="300">
            <template #default="{ row }">
              <span class="song-name-link" @click.stop="$router.push(`/song/${row.id}`)">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="album" label="专辑" width="200">
            <template #default="{ row }">
              <span v-if="row.album" class="album-link" @click.stop="$router.push(`/album/${encodeURIComponent(row.album)}`)">{{ row.album }}</span>
              <span v-else>-</span>
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
        <el-empty v-if="!songs.length" description="暂无歌曲" />
      </div>
    </template>

    <el-empty v-else description="歌手不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { getSingerByName, getSingerSongs } from '@/api/singer'
import { fillMissingDurations } from '@/api/song'

const route = useRoute()
const musicStore = useMusicStore()
const userStore = useUserStore()

const loading = ref(true)
const singer = ref({})
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

onMounted(async () => {
  const name = decodeURIComponent(route.params.name)
  try {
    const [singerRes, songsRes] = await Promise.all([
      getSingerByName(name),
      getSingerSongs(name)
    ])
    singer.value = singerRes.data || {}
    songs.value = songsRes.data || []
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
.singer-detail-page {
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

.singer-header {
  display: flex;
  gap: 30px;
  padding: 30px;
  background: var(--gradient-primary);
  border-radius: 16px;
  margin-bottom: 30px;
  color: #fff;
}

.singer-avatar {
  flex-shrink: 0;
}

.singer-avatar img {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.3);
}

.avatar-placeholder {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4px solid rgba(255, 255, 255, 0.3);
}

.singer-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.singer-name {
  font-size: 32px;
  margin: 0 0 16px 0;
}

.singer-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  opacity: 0.9;
}

.singer-desc {
  font-size: 14px;
  line-height: 1.6;
  opacity: 0.85;
  margin: 0;
}

.singer-desc.placeholder {
  opacity: 0.5;
}

.singer-songs {
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

.album-link {
  color: var(--accent-cyan);
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

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
