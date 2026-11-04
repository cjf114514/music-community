<template>
  <div class="user-profile-page">
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="userInfo">
      <div class="profile-header">
        <el-avatar :size="100" :src="userInfo.avatar">
          {{ userInfo.nickname?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="profile-info">
          <h2>{{ userInfo.nickname || userInfo.username }}</h2>
          <p v-if="userInfo.signature" class="signature">{{ userInfo.signature }}</p>
          <div class="stats">
            <div class="stat-item">
              <span class="stat-num">{{ followStats.followerCount || 0 }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ followStats.followingCount || 0 }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ userStats.playlistCount || 0 }}</span>
              <span class="stat-label">歌单</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ userStats.postCount || 0 }}</span>
              <span class="stat-label">动态</span>
            </div>
          </div>
          <div v-if="!isSelf" class="action-btns">
            <el-button
              :type="isFollowing ? 'default' : 'primary'"
              @click="handleToggleFollow"
            >
              {{ isFollowing ? '已关注' : '关注' }}
            </el-button>
            <el-button type="primary" plain @click="goToMessage">
              <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" style="margin-right:4px;vertical-align:-2px"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/></svg>
              私信
            </el-button>
            <el-dropdown trigger="click" @command="handleMoreAction">
              <el-button circle>
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="!isBlocked" command="block">
                    <span style="color:#ef4444">拉黑用户</span>
                  </el-dropdown-item>
                  <el-dropdown-item v-else command="unblock">取消拉黑</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="profile-tabs">
        <el-tab-pane label="歌单" name="playlists">
          <div v-if="userPlaylists.length" class="playlist-grid">
            <el-row :gutter="20">
              <el-col v-for="pl in userPlaylists" :key="pl.id" :xs="12" :sm="8" :md="6" :lg="4">
                <el-card shadow="hover" class="playlist-card" @click="$router.push(`/playlist/${pl.id}`)">
                  <div class="card-cover">
                    <img v-if="pl.coverUrl" :src="pl.coverUrl" />
                    <div v-else class="cover-placeholder">
                      <el-icon :size="50"><Music /></el-icon>
                    </div>
                  </div>
                  <div class="card-info">
                    <div class="card-name">{{ pl.name }}</div>
                    <div class="card-meta">{{ pl.songCount || 0 }} 首</div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
          <el-empty v-else description="暂无歌单" />
        </el-tab-pane>

        <el-tab-pane label="动态" name="posts">
          <div v-if="userPosts.length" class="posts-list">
            <div v-for="post in userPosts" :key="post.id" class="post-item" @click="$router.push(`/community/post/${post.id}`)">
              <div class="post-content">
                <h4>{{ post.title }}</h4>
                <p>{{ post.content }}</p>
                <div class="post-meta">
                  <span>{{ formatTime(post.createTime) }}</span>
                  <span>{{ post.likeCount || 0 }} 赞</span>
                  <span>{{ post.commentCount || 0 }} 评论</span>
                </div>
              </div>
              <img v-if="post.coverUrl" :src="post.coverUrl" class="post-cover" />
            </div>
          </div>
          <el-empty v-else description="暂无动态" />
        </el-tab-pane>

        <el-tab-pane v-if="isSelf" label="听歌排行" name="history">
          <div v-if="recentSongs.length" class="history-list">
            <div v-for="(song, index) in recentSongs" :key="song.id" class="history-item" @click="playSong(song)">
              <span class="rank-num" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <div class="song-info">
                <span class="song-name">{{ song.name }}</span>
                <span class="song-singer">{{ song.singer }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无听歌记录" />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMusicStore } from '@/stores/music'
import { checkFollowStatus, followUser, unfollowUser, getFollowStats, blockUser, unblockUser, checkBlockStatus } from '@/api/social'
import { getUserPlaylists } from '@/api/song'
import { getUserPosts } from '@/api/community'
import { getRecentPlays } from '@/api/playHistory'
import { getUserStats } from '@/api/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const musicStore = useMusicStore()

const loading = ref(true)
const userInfo = ref(null)
const isFollowing = ref(false)
const isBlocked = ref(false)
const followStats = ref({ followerCount: 0, followingCount: 0 })
const userStats = ref({ playlistCount: 0, postCount: 0, playCount: 0 })
const userPlaylists = ref([])
const userPosts = ref([])
const recentSongs = ref([])
const activeTab = ref('playlists')

const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => userId.value === userStore.userInfo?.userId)

onMounted(async () => {
  await loadUserProfile()
})

const loadUserProfile = async () => {
  loading.value = true
  try {
    const [playlistsRes, statsRes, followStatsRes] = await Promise.all([
      getUserPlaylists(userId.value),
      getUserStats(userId.value),
      getFollowStats(userId.value)
    ])
    userPlaylists.value = playlistsRes.data || []
    userStats.value = statsRes.data || {}
    followStats.value = followStatsRes.data || {}

    if (!isSelf.value) {
      const [statusRes, blockRes] = await Promise.all([
        checkFollowStatus(userId.value),
        checkBlockStatus(userId.value)
      ])
      isFollowing.value = statusRes.data
      isBlocked.value = blockRes.data
    }

    if (isSelf.value) {
      try {
        const historyRes = await getRecentPlays(20)
        recentSongs.value = historyRes.data || []
      } catch (e) {}
    }

    userInfo.value = {
      id: userId.value,
      nickname: route.query.nickname || (isSelf.value ? userStore.userInfo?.nickname : null) || '用户',
      avatar: route.query.avatar || (isSelf.value ? userStore.userInfo?.avatar : null) || null,
      username: route.query.username || (isSelf.value ? userStore.userInfo?.username : '') || '',
      signature: route.query.signature || (isSelf.value ? userStore.userInfo?.signature : null) || ''
    }
  } catch (e) {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const loadUserPosts = async () => {
  if (userPosts.value.length) return
  try {
    const res = await getUserPosts(userId.value, 20)
    userPosts.value = res.data || []
  } catch (e) {}
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return d.toLocaleDateString()
}

const playSong = (song) => {
  musicStore.setCurrentSong(song)
}

const handleToggleFollow = async () => {
  try {
    if (isFollowing.value) {
      await unfollowUser(userId.value)
      ElMessage.success('已取消关注')
    } else {
      await followUser(userId.value)
      ElMessage.success('关注成功')
    }
    isFollowing.value = !isFollowing.value
    const statsRes = await getFollowStats(userId.value)
    followStats.value = statsRes.data || {}
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const goToMessage = () => {
  router.push({ path: '/messages', query: { to: userId.value } })
}

const handleMoreAction = async (cmd) => {
  if (cmd === 'block') {
    try {
      await ElMessageBox.confirm('拉黑后对方无法给你发送私信，你也将自动取消关注对方', '确认拉黑', {
        confirmButtonText: '拉黑',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await blockUser(userId.value)
      isBlocked.value = true
      isFollowing.value = false
      ElMessage.success('已拉黑')
      const statsRes = await getFollowStats(userId.value)
      followStats.value = statsRes.data || {}
    } catch (e) {
      if (e !== 'cancel') ElMessage.error('操作失败')
    }
  } else if (cmd === 'unblock') {
    try {
      await unblockUser(userId.value)
      isBlocked.value = false
      ElMessage.success('已取消拉黑')
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }
}

import { watch } from 'vue'
watch(activeTab, (val) => {
  if (val === 'posts') loadUserPosts()
})
</script>

<style scoped>
.user-profile-page {
  padding: 20px 0;
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

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 30px;
  background: var(--bg-secondary);
  border-radius: 12px;
  margin-bottom: 24px;
}

.profile-info h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
}

.signature {
  color: var(--text-secondary);
  margin: 0 0 16px;
  font-size: 14px;
}

.stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 20px;
  font-weight: bold;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-tabs {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 20px;
}

.playlist-grid {
  margin-top: 16px;
}

.playlist-card {
  margin-bottom: 20px;
  cursor: pointer;
}

.card-cover {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 8px;
}

.card-cover img {
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

.card-info {
  padding: 12px 0 0;
}

.card-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.posts-list {
  margin-top: 8px;
}

.post-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  border-radius: 10px;
  background: var(--bg-elevated);
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--border-color);
}

.post-item:hover {
  border-color: var(--border-glow);
  box-shadow: var(--neon-glow-sm);
}

.post-content {
  flex: 1;
  min-width: 0;
}

.post-content h4 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-content p {
  margin: 0 0 10px;
  color: var(--text-secondary);
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.post-cover {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  margin-left: 16px;
  flex-shrink: 0;
}

.history-list {
  margin-top: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 10px;
  background: var(--bg-elevated);
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--border-color);
}

.history-item:hover {
  border-color: var(--border-glow);
  box-shadow: var(--neon-glow-sm);
}

.rank-num {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: bold;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  flex-shrink: 0;
}

.rank-num.top {
  background: var(--gradient-primary);
  color: #fff;
}

.song-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.song-name {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-singer {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}
</style>
