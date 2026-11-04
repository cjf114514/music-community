<template>
  <div class="my-favorites" v-loading="loading">
    <div class="favorites-header">
      <div class="favorites-cover">
        <div class="cover-gradient">
          <el-icon :size="60"><StarFilled /></el-icon>
        </div>
      </div>
      <div class="favorites-meta">
        <h1 class="favorites-title">我喜欢的音乐</h1>
        <p class="favorites-count">{{ songs.length }} 首歌曲</p>
        <div class="favorites-actions">
          <el-button type="primary" size="large" @click="playAll">
            <el-icon><VideoPlay /></el-icon> 播放全部
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="songs.length" class="song-list">
      <div class="song-list-header">
        <span class="col-index">#</span>
        <span class="col-title">标题</span>
        <span class="col-album">专辑</span>
        <span class="col-like">喜欢</span>
        <span class="col-duration">时长</span>
        <span class="col-action">操作</span>
      </div>
      <div
        v-for="(song, index) in paginatedSongs"
        :key="song.id"
        class="song-item"
        :class="{ active: store.currentSong?.id === song.id }"
        @dblclick="playSong(song, index)"
      >
        <span class="col-index">{{ (currentPage - 1) * pageSize + index + 1 }}</span>
        <span class="col-title">
          <div class="song-thumb">
            <img v-if="song.coverUrl" :src="song.coverUrl" />
            <div v-else class="thumb-placeholder" :style="{ background: getGradient(index) }">
              <el-icon :size="16"><Music /></el-icon>
            </div>
          </div>
          <div class="song-info">
            <span class="song-name" @click.stop="$router.push(`/song/${song.id}`)">{{ song.name }}</span>
            <span class="song-singer" @click.stop="$router.push(`/singer/${encodeURIComponent(song.singer)}`)">{{ song.singer }}</span>
          </div>
        </span>
        <span class="col-album" @click.stop="$router.push(`/album/${encodeURIComponent(song.album)}`)">{{ song.album }}</span>
        <span class="col-like" @click.stop="handleToggleFavorite(song)">
          <el-icon class="favorited">
            <StarFilled />
          </el-icon>
        </span>
        <span class="col-duration">{{ formatDuration(song.duration) }}</span>
        <span class="col-action" @click.stop>
          <div class="action-btn" @click="playSong(song)" title="播放">
            <el-icon><VideoPlay /></el-icon>
          </div>
          <div class="action-btn" @click="handleAddToQueue(song)" title="添加到播放列表">
            <el-icon><CirclePlus /></el-icon>
          </div>
          <el-dropdown trigger="click" @command="(cmd) => handleAddToPlaylist(cmd, song.id)">
            <div class="action-btn" @click.stop>
              <el-icon><Plus /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="pl in myPlaylists" :key="pl.id" :command="pl.id">
                  {{ pl.name }}
                </el-dropdown-item>
                <el-dropdown-item divided command="create">
                  <el-icon><Plus /></el-icon> 创建新歌单
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </span>
      </div>
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

    <el-empty v-else description="还没有喜欢的音乐，快去发现吧" />

    <el-dialog v-model="showCreateDialog" title="创建新歌单并添加歌曲" width="450px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="歌单名称" required>
          <el-input v-model="createForm.name" placeholder="请输入歌单名称" />
        </el-form-item>
        <el-form-item label="歌单描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateAndAdd">创建并添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getUserPlaylists, addSongToPlaylist, createPlaylist, fillMissingDurations } from '@/api/song'
import { getMyFavorites, toggleFavorite } from '@/api/favorite'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useMusicStore()
const userStore = useUserStore()

const songs = ref([])
const myPlaylists = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const creating = ref(false)
const pendingSongId = ref(null)
const currentPage = ref(1)
const pageSize = 12

const paginatedSongs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return songs.value.slice(start, start + pageSize)
})

const gradients = [
  'linear-gradient(135deg, #ec4141 0%, #ff6b6b 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
]

const createForm = ref({ name: '', description: '', isPublic: 1, userId: null })

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMyFavorites()
    songs.value = res.data || []
    store.setPlayList(songs.value)
    fillMissingDurations(songs.value)
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }

  if (userStore.userInfo?.userId) {
    const plRes = await getUserPlaylists(userStore.userInfo.userId)
    myPlaylists.value = plRes.data || []
  }
})

const playSong = (song, index) => {
  store.setPlayList(songs.value)
  store.setCurrentSong(song)
}

const handleAddToQueue = (song) => {
  const added = store.addToQueue(song)
  if (added) {
    ElMessage.success('已添加到播放列表')
  } else {
    ElMessage.info('歌曲已在播放列表中')
  }
}

const playAll = () => {
  if (songs.value.length === 0) return
  store.setPlayList(songs.value)
  store.setCurrentSong(songs.value[0])
}

const getGradient = (index) => {
  return gradients[index % gradients.length]
}

const formatDuration = (seconds) => {
  if (!seconds) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

const handleToggleFavorite = async (song) => {
  try {
    await toggleFavorite(song.id)
    songs.value = songs.value.filter(s => s.id !== song.id)
    ElMessage.success('已取消喜欢')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleAddToPlaylist = (playlistId, songId) => {
  if (playlistId === 'create') {
    pendingSongId.value = songId
    createForm.value = { name: '', description: '', isPublic: 1, userId: userStore.userInfo?.id }
    showCreateDialog.value = true
    return
  }
  doAddSong(playlistId, songId)
}

const handleCreateAndAdd = async () => {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入歌单名称')
    return
  }
  if (!userStore.userInfo?.userId) {
    ElMessage.error('请先登录')
    return
  }
  creating.value = true
  try {
    const payload = {
      name: createForm.value.name,
      description: createForm.value.description,
      isPublic: 1,
      userId: userStore.userInfo.userId
    }
    const res = await createPlaylist(payload)
    await addSongToPlaylist(res.data.id, pendingSongId.value)
    ElMessage.success('创建并添加成功')
    showCreateDialog.value = false
    const plRes = await getUserPlaylists(userStore.userInfo.userId)
    myPlaylists.value = plRes.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    creating.value = false
  }
}

const doAddSong = async (playlistId, songId) => {
  try {
    await addSongToPlaylist(playlistId, songId)
    ElMessage.success('已添加到歌单')
    const plRes = await getUserPlaylists(userStore.userInfo.userId)
    myPlaylists.value = plRes.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '添加失败')
  }
}
</script>

<style scoped>
.my-favorites {
  padding: 20px 0;
  min-height: calc(100vh - 160px);
  background: var(--bg-primary);
  color: var(--text-primary);
}

.song-list {
  background: var(--bg-secondary);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
}

.song-list-header {
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-color);
}

.favorites-header {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
}

.favorites-cover {
  width: 200px;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.cover-gradient {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ec4141 0%, #ff6b6b 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8);
}

.favorites-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.favorites-title {
  font-size: 28px;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.favorites-count {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 20px;
}

.favorites-actions {
  display: flex;
  gap: 12px;
}

.song-list-header {
  display: grid;
  grid-template-columns: 50px 1fr 200px 60px 80px 100px;
  padding: 12px 20px;
  font-size: 13px;
  color: var(--text-secondary);
}

.song-item {
  display: grid;
  grid-template-columns: 50px 1fr 200px 60px 80px 100px;
  padding: 8px 20px;
  align-items: center;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  transition: background 0.2s;
}

.song-item:hover {
  background: var(--nav-hover-bg);
}

.song-item.active {
  color: var(--primary-color);
}

.col-index {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--text-secondary);
}

.col-title {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.song-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.song-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8);
}

.song-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.song-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.song-name:hover {
  color: var(--primary-color);
}

.song-singer {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.song-singer:hover {
  color: var(--primary-color);
}

.col-album {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.col-album:hover {
  color: var(--primary-color);
}

.col-like {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  cursor: pointer;
  transition: color 0.2s;
}

.col-like:hover {
  opacity: 0.8;
}

.col-duration {
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
}

.col-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.col-action .action-btn {
  padding: 4px;
  border-radius: 4px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.col-action .action-btn:hover {
  background: var(--nav-hover-bg);
  color: var(--primary-color);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
