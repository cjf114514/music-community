<template>
  <div class="shared-song-page">
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="song">
      <div class="song-header">
        <div class="song-cover">
          <el-image v-if="song.coverUrl" :src="song.coverUrl" fit="cover" />
          <div v-else class="cover-placeholder">
            <el-icon :size="60"><Music /></el-icon>
          </div>
        </div>
        <div class="song-info">
          <p class="sharer-name">{{ song.sharerNickname }} 分享的歌曲</p>
          <h2>{{ song.name }}</h2>
          <p class="song-meta">{{ song.singer }} - {{ song.album }}</p>
          <div class="song-stats">
            <span><el-icon><Share /></el-icon> 分享 {{ song.shareCount }} 次</span>
          </div>
        </div>
      </div>

      <div class="song-actions">
        <el-button type="primary" @click="playSong">
          <el-icon><VideoPlay /></el-icon> 播放歌曲
        </el-button>
        <el-button @click="viewSongDetail">
          <el-icon><View /></el-icon> 查看详情
        </el-button>
      </div>
    </template>

    <el-empty v-else description="分享链接无效" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSharedSong } from '@/api/social'
import { useMusicStore } from '@/stores/music'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useMusicStore()
const loading = ref(true)
const song = ref(null)

onMounted(async () => {
  await loadSharedSong()
})

const loadSharedSong = async () => {
  loading.value = true
  try {
    const res = await getSharedSong(route.params.token)
    song.value = res.data
  } catch (e) {
    ElMessage.error('分享链接无效')
  } finally {
    loading.value = false
  }
}

const playSong = () => {
  store.playSong(song.value)
}

const viewSongDetail = () => {
  router.push(`/song/${song.value.id}`)
}
</script>

<style scoped>
.shared-song-page {
  padding: 60px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 0;
  color: var(--text-secondary);
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.song-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
  text-align: center;
}

.song-cover {
  width: 180px;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.song-cover img {
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
  color: rgba(255, 255, 255, 0.6);
}

.song-info {
  text-align: left;
}

.sharer-name {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0 0 8px;
}

.song-info h2 {
  color: var(--text-primary);
  margin: 0 0 8px;
}

.song-meta {
  color: var(--text-secondary);
  margin: 0 0 16px;
}

.song-stats {
  display: flex;
  gap: 16px;
  color: var(--text-secondary);
  font-size: 14px;
}

.song-actions {
  display: flex;
  gap: 16px;
}
</style>
