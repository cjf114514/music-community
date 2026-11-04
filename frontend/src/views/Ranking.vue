<template>
  <div class="ranking-page">
    <div class="ranking-header">
      <h2 class="page-title">歌曲排行榜</h2>
      <el-radio-group v-model="rankingType" @change="loadRanking">
        <el-radio-button value="daily">日榜</el-radio-button>
        <el-radio-button value="weekly">周榜</el-radio-button>
        <el-radio-button value="total">总榜</el-radio-button>
      </el-radio-group>
    </div>

    <div class="ranking-list" v-loading="loading">
      <div class="ranking-table-wrapper">
        <div class="table-header">
          <span class="col-index">#</span>
          <span class="col-title">标题</span>
          <span class="col-album">专辑</span>
          <span v-if="userStore.isLoggedIn()" class="col-like">喜欢</span>
          <span class="col-duration">时长</span>
          <span class="col-action">操作</span>
        </div>
        <div
          v-for="(song, index) in paginatedSongs"
          :key="song.id"
          class="song-row"
          :class="{ active: store.currentSong?.id === song.id }"
          @click="playSong(song)"
        >
          <span class="col-index">
            <span :class="['rank-num', getRankClass(index)]">{{ (currentPage - 1) * pageSize + index + 1 }}</span>
          </span>
          <span class="col-title">
            <div class="song-thumb">
              <img v-if="song.coverUrl" :src="song.coverUrl" />
              <div v-else class="thumb-placeholder" :style="{ background: getGradient(index) }">
                <el-icon :size="16"><Music /></el-icon>
              </div>
            </div>
            <div class="song-info">
              <span class="song-name" @click.stop="$router.push(`/song/${song.id}`)">{{ song.name }}</span>
              <span class="song-singer">{{ song.singer }}</span>
            </div>
          </span>
          <span class="col-album">{{ song.album }}</span>
          <span v-if="userStore.isLoggedIn()" class="col-like" @click.stop="handleToggleFavorite(song)">
            <el-icon :class="{ favorited: favoriteMap[song.id] }">
              <StarFilled v-if="favoriteMap[song.id]" />
              <Star v-else />
            </el-icon>
          </span>
          <span class="col-duration">{{ formatDuration(song.duration) }}</span>
          <span class="col-action" @click.stop>
            <div class="action-btn" @click="playSong(song)" title="播放">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div v-if="userStore.isLoggedIn()" class="action-btn" @click="handleAddToQueue(song)" title="添加到播放列表">
              <el-icon><CirclePlus /></el-icon>
            </div>
            <el-dropdown v-if="userStore.isLoggedIn()" trigger="click" @command="(cmd) => handleAddToPlaylist(cmd, song.id)">
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
      </div>
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage"
          @update:current-page="currentPage = $event"
          :page-size="pageSize"
          :total="rankingSongs.length"
          layout="prev, pager, next"
          background
          small
        />
      </div>
      <el-empty v-if="!loading && !rankingSongs.length" description="暂无数据" />
    </div>

    <el-dialog v-model="showCreateDialog" title="创建新歌单并添加歌曲" width="450px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="歌单名称">
          <el-input placeholder="请输入歌单名称" />
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
import { getRanking } from '@/api/playHistory'
import { getUserPlaylists, addSongToPlaylist, createPlaylist, fillMissingDurations } from '@/api/song'
import { toggleFavorite, checkFavorite } from '@/api/favorite'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useMusicStore()
const userStore = useUserStore()
const rankingType = ref('total')
const rankingSongs = ref([])
const myPlaylists = ref([])
const loading = ref(false)
const favoriteMap = ref({})
const showCreateDialog = ref(false)
const creating = ref(false)
const pendingSongId = ref(null)
const currentPage = ref(1)
const pageSize = 12

const paginatedSongs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return rankingSongs.value.slice(start, start + pageSize)
})

const gradients = [
  'linear-gradient(135deg, #ec4141 0%, #ff6b6b 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
]

const getGradient = (index) => {
  return gradients[index % gradients.length]
}

const formatDuration = (seconds) => {
  if (!seconds) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

onMounted(async () => {
  loadRanking()
  if (userStore.userInfo?.userId) {
    const plRes = await getUserPlaylists(userStore.userInfo.userId)
    myPlaylists.value = plRes.data || []
  }
})

const loadRanking = async () => {
  loading.value = true
  currentPage.value = 1
  try {
    const res = await getRanking(rankingType.value, 50)
    rankingSongs.value = res.data || []
    store.setPlayList(rankingSongs.value)
    fillMissingDurations(rankingSongs.value)
    if (userStore.isLoggedIn()) {
      await loadFavorites()
    }
  } catch (e) {
    ElMessage.error('加载排行榜失败')
  } finally {
    loading.value = false
  }
}

const loadFavorites = async () => {
  try {
    for (const song of rankingSongs.value) {
      const res = await checkFavorite(song.id)
      favoriteMap.value[song.id] = res.data
    }
  } catch (e) {}
}

const playSong = (song) => {
  store.setCurrentSong(song)
  store.setPlayList(rankingSongs.value)
}

const handleAddToQueue = (song) => {
  const added = store.addToQueue(song)
  if (added) {
    ElMessage.success('已添加到播放列表')
  } else {
    ElMessage.info('歌曲已在播放列表中')
  }
}

const getRankClass = (index) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

const handleToggleFavorite = async (song) => {
  try {
    await toggleFavorite(song.id)
    favoriteMap.value[song.id] = !favoriteMap.value[song.id]
    ElMessage.success(favoriteMap.value[song.id] ? '已添加到我喜欢的音乐' : '已取消喜欢')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleAddToPlaylist = (playlistId, songId) => {
  if (playlistId === 'create') {
    pendingSongId.value = songId
    showCreateDialog.value = true
    return
  }
  doAddSong(playlistId, songId)
}

const handleCreateAndAdd = async () => {
  if (!userStore.userInfo?.userId) {
    ElMessage.error('请先登录')
    return
  }
  creating.value = true
  try {
    const payload = {
      name: '新歌单',
      description: '',
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
.ranking-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.ranking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.ranking-list {
  background: var(--bg-secondary);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
}

.ranking-table-wrapper {
  background: var(--bg-secondary);
  border-radius: 16px;
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 50px 1fr 180px 60px 80px 100px;
  padding: 14px 20px;
  background: var(--bg-elevated);
  font-size: 12px;
  color: var(--text-tertiary);
  border-bottom: 1px solid var(--border-color);
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
}

.song-row {
  display: grid;
  grid-template-columns: 50px 1fr 180px 60px 80px 100px;
  padding: 10px 20px;
  align-items: center;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid rgba(99, 102, 241, 0.04);
}

.song-row:hover {
  background: var(--nav-hover-bg);
}

.song-row.active {
  color: var(--primary-color);
  background: var(--nav-active-bg);
}

.col-index {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}

.rank-num {
  font-weight: 700;
}

.rank-1 {
  color: var(--accent-pink);
  font-size: 16px;
  text-shadow: 0 0 8px rgba(244, 114, 182, 0.4);
}

.rank-2 {
  color: var(--accent-amber);
  font-size: 15px;
  text-shadow: 0 0 8px rgba(251, 191, 36, 0.3);
}

.rank-3 {
  color: var(--accent-cyan);
  font-size: 14px;
  text-shadow: 0 0 8px rgba(34, 211, 238, 0.3);
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
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
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
}

.col-album {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.col-like {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color 0.2s;
}

.col-like:hover {
  color: var(--primary-color);
}

.col-like .favorited {
  color: var(--accent-pink);
}

.col-duration {
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.col-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.col-action .action-btn {
  padding: 4px;
  border-radius: 6px;
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
