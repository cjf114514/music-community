<template>
  <div class="shared-playlist-page">
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="playlist">
      <div class="playlist-header">
        <el-avatar :size="80" :src="playlist.sharerAvatar">
          {{ playlist.sharerNickname?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="playlist-info">
          <p class="sharer-name">{{ playlist.sharerNickname }} 分享的歌单</p>
          <h2>{{ playlist.name }}</h2>
          <p class="playlist-desc">{{ playlist.description || '暂无描述' }}</p>
          <div class="playlist-stats">
            <span><el-icon><Share /></el-icon> 分享 {{ playlist.shareCount }} 次</span>
            <span><el-icon><Music /></el-icon> {{ playlist.songCount }} 首歌曲</span>
          </div>
        </div>
      </div>

      <el-button type="primary" @click="viewPlaylist">
        <el-icon><View /></el-icon> 查看歌单
      </el-button>
    </template>

    <el-empty v-else description="分享链接无效" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSharedPlaylist } from '@/api/social'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const playlist = ref(null)

onMounted(async () => {
  await loadSharedPlaylist()
})

const loadSharedPlaylist = async () => {
  loading.value = true
  try {
    const res = await getSharedPlaylist(route.params.token)
    playlist.value = res.data
  } catch (e) {
    ElMessage.error('分享链接无效')
  } finally {
    loading.value = false
  }
}

const viewPlaylist = () => {
  router.push(`/playlist/${playlist.value.id}`)
}
</script>

<style scoped>
.shared-playlist-page {
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

.playlist-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
  text-align: center;
}

.playlist-info {
  text-align: left;
}

.sharer-name {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0 0 8px;
}

.playlist-info h2 {
  color: var(--text-primary);
  margin: 0 0 8px;
}

.playlist-desc {
  color: var(--text-secondary);
  margin: 0 0 16px;
}

.playlist-stats {
  display: flex;
  gap: 16px;
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
