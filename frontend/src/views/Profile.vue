<template>
  <div class="profile">
    <h2 class="page-title">个人中心</h2>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <div class="profile-info">
            <div class="avatar-wrapper">
              <el-avatar :size="80" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <div class="change-avatar-btn">
                  <el-icon><Camera /></el-icon> 更换头像
                </div>
              </el-upload>
            </div>
            <h3>{{ userStore.userInfo?.nickname }}</h3>
            <p>@{{ userStore.userInfo?.username }}</p>
            <p v-if="userStore.userInfo?.email">{{ userStore.userInfo?.email }}</p>
            <p v-if="userStore.userInfo?.signature">{{ userStore.userInfo?.signature }}</p>
            <div class="profile-links">
              <div class="profile-link" @click="router.push('/my-playlists')">
                <el-icon><Music /></el-icon> 我的歌单
              </div>
              <div class="profile-link" @click="router.push(`/user/${userStore.userInfo?.userId}`)">
                <el-icon><User /></el-icon> 我的主页
              </div>
              <div class="profile-link" @click="router.push('/edit-profile')">
                <el-icon><Edit /></el-icon> 编辑资料
              </div>
              <div class="profile-link" @click="router.push('/account-settings')">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                账号安全
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <el-tabs v-model="activeTab">
              <el-tab-pane label="我的收藏" name="favorites" />
              <el-tab-pane label="最近播放" name="recent" />
            </el-tabs>
          </template>

          <div v-show="activeTab === 'favorites'">
            <el-table :data="myFavorites" stripe style="width: 100%" @row-click="playSong">
              <el-table-column prop="name" label="歌曲" width="300" />
              <el-table-column prop="singer" label="歌手" width="200">
                <template #default="{ row }">
                  <span class="singer-link" @click.stop="$router.push(`/singer/${encodeURIComponent(row.singer)}`)">{{ row.singer }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="album" label="专辑" width="200">
                <template #default="{ row }">
                  <span class="album-link" @click.stop="$router.push(`/album/${encodeURIComponent(row.album)}`)">{{ row.album }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <div class="action-btn" @click.stop="playSong(row)">
                    <el-icon><VideoPlay /></el-icon> 播放
                  </div>
                  <div class="action-btn" @click.stop="handleUnfavorite(row.id)">
                    <el-icon><Star /></el-icon> 取消收藏
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!myFavorites.length" description="暂无收藏" />
          </div>

          <div v-show="activeTab === 'recent'">
            <el-table :data="recentPlays" stripe style="width: 100%" @row-click="playSong">
              <el-table-column prop="name" label="歌曲" width="300" />
              <el-table-column prop="singer" label="歌手" width="200">
                <template #default="{ row }">
                  <span class="singer-link" @click.stop="$router.push(`/singer/${encodeURIComponent(row.singer)}`)">{{ row.singer }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="album" label="专辑" width="200">
                <template #default="{ row }">
                  <span class="album-link" @click.stop="$router.push(`/album/${encodeURIComponent(row.album)}`)">{{ row.album }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <div class="action-btn" @click.stop="playSong(row)">
                    <el-icon><VideoPlay /></el-icon> 播放
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!recentPlays.length" description="暂无播放记录" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserInfo, updateUserInfo } from '@/api/auth'
import { getMyFavorites, toggleFavorite } from '@/api/favorite'
import { getRecentPlays } from '@/api/playHistory'
import { uploadAvatar } from '@/api/song'
import { useUserStore } from '@/stores/user'
import { useMusicStore } from '@/stores/music'

const router = useRouter()
const userStore = useUserStore()
const musicStore = useMusicStore()
const myFavorites = ref([])
const recentPlays = ref([])
const activeTab = ref('favorites')

onMounted(async () => {
  try {
    const res = await getUserInfo()
    const info = res.data
    userStore.setUserInfo({ ...userStore.userInfo, ...info })
  } catch (e) {
    ElMessage.error('获取用户信息失败')
  }

  await loadMyFavorites()
  await loadRecentPlays()
})

const loadMyFavorites = async () => {
  try {
    const res = await getMyFavorites()
    myFavorites.value = res.data || []
  } catch (e) {
    ElMessage.error('获取收藏列表失败')
  }
}

const loadRecentPlays = async () => {
  try {
    const res = await getRecentPlays(20)
    recentPlays.value = res.data || []
  } catch (e) {}
}

const handleUnfavorite = async (songId) => {
  try {
    await toggleFavorite(songId)
    ElMessage.success('已取消收藏')
    await loadMyFavorites()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const playSong = (song) => {
  musicStore.setCurrentSong(song)
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options) => {
  try {
    const res = await uploadAvatar(options.file)
    const avatarUrl = res.data
    await updateUserInfo({ avatar: avatarUrl })
    userStore.setUserInfo({ ...userStore.userInfo, avatar: avatarUrl })
    ElMessage.success('头像更新成功')
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
}
</script>

<style scoped>
.profile {
  padding: 20px 0;
}

.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: var(--text-primary);
  font-weight: 700;
}

.profile-info {
  text-align: center;
}

.avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.avatar-uploader {
  display: inline-block;
}

.profile-info h3 {
  margin: 16px 0 8px;
}

.profile-info p {
  color: var(--text-secondary);
  margin: 4px 0;
}

.profile-links {
  margin-top: 16px;
}

.user-data-section {
  margin-top: 20px;
}

.section-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.el-table {
  cursor: pointer;
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

.profile-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
}

.profile-link {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.profile-link:hover {
  color: var(--primary-color);
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

.change-avatar-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.change-avatar-btn:hover {
  color: var(--primary-color);
}

.clear-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.clear-btn:hover:not(.disabled) {
  color: var(--primary-color);
}

.clear-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.action-btn:hover {
  color: var(--primary-color);
}
</style>
