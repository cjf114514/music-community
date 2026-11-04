<template>
  <div class="search-page">
    <div class="search-input-section">
      <el-input
        v-model="keyword"
        placeholder="搜索歌曲、歌手、专辑"
        size="large"
        clearable
        @keyup.enter="handleSearch"
        @focus="showDropdown = true"
        @blur="handleBlur"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <div class="search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </div>
        </template>
      </el-input>

      <div v-if="showDropdown && !hasSearched" class="search-dropdown">
        <div v-if="hotSearch.length" class="dropdown-section">
          <div class="section-title">
            <el-icon><TrendCharts /></el-icon> 热门搜索
          </div>
          <div class="hot-tags">
            <el-tag
              v-for="tag in hotSearch"
              :key="tag"
              size="default"
              @click="searchByTag(tag)"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
        <div v-if="searchHistory.length" class="dropdown-section">
          <div class="section-title">
            <el-icon><Clock /></el-icon> 搜索历史
            <div class="clear-btn" @click="handleClearHistory">清空</div>
          </div>
          <div class="history-list">
            <div
              v-for="item in searchHistory"
              :key="item"
              class="history-item"
              @click="searchByTag(item)"
            >
              <el-icon><Clock /></el-icon>
              <span>{{ item }}</span>
              <el-icon class="delete-icon" @click.stop="handleDeleteHistory(item)"><Close /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="hasSearched" class="search-results">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="歌曲" name="songs">
          <div class="song-list">
            <div class="song-list-header">
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
              :total="songResults.length"
              layout="prev, pager, next"
              background
              small
            />
          </div>
          <el-empty v-if="!songResults.length" description="未找到相关歌曲" />
        </el-tab-pane>

        <el-tab-pane label="歌单" name="playlists">
          <div class="playlist-results">
            <div
              v-for="(pl, index) in playlistResults"
              :key="pl.id"
              class="playlist-card"
              @click="$router.push(`/playlist/${pl.id}`)"
            >
              <div class="playlist-cover">
                <img v-if="pl.coverUrl" :src="pl.coverUrl" />
                <div v-else class="cover-placeholder" :style="{ background: getGradient(index) }">
                  <el-icon :size="40"><Music /></el-icon>
                </div>
              </div>
              <div class="playlist-info">
                <div class="playlist-name">{{ pl.name }}</div>
                <div class="playlist-desc">{{ pl.description || '暂无描述' }}</div>
              </div>
            </div>
          </div>
          <el-empty v-if="!playlistResults.length" description="未找到相关歌单" />
        </el-tab-pane>

        <el-tab-pane label="歌手" name="singers">
          <div class="singer-results">
            <div
              v-for="singer in singerResults"
              :key="singer.name"
              class="singer-card"
              @click="$router.push(`/singer/${encodeURIComponent(singer.name)}`)"
            >
              <div class="singer-avatar">
                <img v-if="singer.avatar" :src="singer.avatar" />
                <div v-else class="avatar-placeholder">
                  <el-icon :size="32"><UserFilled /></el-icon>
                </div>
              </div>
              <div class="singer-info">
                <div class="singer-name">{{ singer.name }}</div>
                <div class="singer-count">{{ singer.songCount || 0 }} 首歌曲</div>
              </div>
            </div>
          </div>
          <el-empty v-if="!singerResults.length" description="未找到相关歌手" />
        </el-tab-pane>

        <el-tab-pane label="帖子" name="posts">
          <div class="post-results">
            <div
              v-for="post in postResults"
              :key="post.id"
              class="post-card"
              @click="$router.push(`/community/post/${post.id}`)"
            >
              <div class="post-card-header">
                <el-avatar :size="32" :src="post.userAvatar">
                  {{ post.userNickname?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="post-card-user">
                  <span class="post-card-name">{{ post.userNickname }}</span>
                </div>
              </div>
              <div class="post-card-title">{{ post.title }}</div>
              <div class="post-card-content">{{ post.content }}</div>
              <div class="post-card-stats">
                <span><el-icon><Star /></el-icon> {{ post.likeCount || 0 }}</span>
                <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }}</span>
              </div>
            </div>
          </div>
          <el-empty v-if="!postResults.length" description="未找到相关帖子" />
        </el-tab-pane>

        <el-tab-pane label="用户" name="users">
          <div class="user-results">
            <div
              v-for="user in userResults"
              :key="user.id"
              class="user-card"
            >
              <div class="user-avatar" @click="$router.push(`/user/${user.id}`)">
                <img v-if="user.avatar" :src="user.avatar" />
                <div v-else class="avatar-placeholder">{{ (user.nickname || user.username || 'U').charAt(0) }}</div>
              </div>
              <div class="user-info" @click="$router.push(`/user/${user.id}`)">
                <div class="user-name">{{ user.nickname || user.username }}</div>
                <div class="user-signature" v-if="user.signature">{{ user.signature }}</div>
              </div>
              <div class="user-actions" v-if="userStore.isLoggedIn() && userStore.userInfo && user.id !== Number(userStore.userInfo.userId)">
                <el-button
                  :type="followMap[user.id] ? 'default' : 'primary'"
                  size="small"
                  round
                  @click.stop="handleToggleFollowUser(user)"
                >
                  {{ followMap[user.id] ? '已关注' : '关注' }}
                </el-button>
                <el-button size="small" round plain @click.stop="goToMessage(user)">
                  私信
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-if="!userResults.length" description="未找到相关用户" />
        </el-tab-pane>
      </el-tabs>
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
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchSongs, searchPlaylists, getUserPlaylists, addSongToPlaylist, createPlaylist, searchUsers, searchSingers, fillMissingDurations } from '@/api/song'
import { getSearchHistory, clearSearchHistory, deleteSearchHistory, getHotSearch, recordSearch } from '@/api/search'
import { toggleFavorite, checkFavorite } from '@/api/favorite'
import { followUser, checkFollowStatus } from '@/api/social'
import { searchPosts } from '@/api/community'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useMusicStore()
const userStore = useUserStore()

const keyword = ref(route.query.keyword || '')
const activeTab = ref('songs')
const showDropdown = ref(false)
const hasSearched = ref(false)

const songResults = ref([])
const playlistResults = ref([])
const userResults = ref([])
const singerResults = ref([])
const postResults = ref([])
const followMap = ref({})
const searchHistory = ref([])
const hotSearch = ref([])
const myPlaylists = ref([])
const favoriteMap = ref({})
const showCreateDialog = ref(false)
const creating = ref(false)
const pendingSongId = ref(null)
const currentPage = ref(1)
const pageSize = 12

const paginatedSongs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return songResults.value.slice(start, start + pageSize)
})

const gradients = [
  'linear-gradient(135deg, #ec4141 0%, #ff6b6b 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
]

let clickTimer = null

onMounted(async () => {
  await loadHotSearch()
  if (userStore.userInfo?.userId) {
    await loadSearchHistory()
    const plRes = await getUserPlaylists(userStore.userInfo.userId)
    myPlaylists.value = plRes.data || []
  }
  if (route.query.keyword) {
    handleSearch()
  }
})

onUnmounted(() => {
  clearTimeout(clickTimer)
})

const handleBlur = () => {
  clickTimer = setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

const loadHotSearch = async () => {
  try {
    const res = await getHotSearch(10)
    hotSearch.value = res.data || []
  } catch (e) {}
}

const loadSearchHistory = async () => {
  try {
    const res = await getSearchHistory(10)
    searchHistory.value = res.data || []
  } catch (e) {}
}

const handleSearch = async () => {
  if (!keyword.value.trim()) return
  showDropdown.value = false
  hasSearched.value = true
  currentPage.value = 1

  if (userStore.userInfo?.userId) {
    recordSearch(keyword.value.trim()).catch(() => {})
  }

  router.push({ query: { keyword: keyword.value.trim() } })

  const kw = keyword.value.trim()
  const songRes = await searchSongs(kw)
  songResults.value = songRes.data || []
  store.setPlayList(songResults.value)
  fillMissingDurations(songResults.value)
  if (userStore.isLoggedIn()) {
    await loadFavorites()
  }

  const plRes = await searchPlaylists(kw)
  playlistResults.value = plRes.data || []

  const userRes = await searchUsers(kw)
  userResults.value = userRes.data?.list || []
  followMap.value = {}
  if (userStore.userInfo?.userId) {
    for (const u of userResults.value) {
      try { followMap.value[u.id] = (await checkFollowStatus(u.id)).data } catch(e) {}
    }
  }

  const singerRes = await searchSingers(kw)
  singerResults.value = singerRes.data || []

  const postRes = await searchPosts(kw)
  postResults.value = postRes.data || []
}

const loadFavorites = async () => {
  try {
    for (const song of songResults.value) {
      const res = await checkFavorite(song.id)
      favoriteMap.value[song.id] = res.data
    }
  } catch (e) {}
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

const formatDuration = (seconds) => {
  if (!seconds) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

const handleToggleFollowUser = async (user) => {
  if (!userStore.userInfo?.userId) { ElMessage.error('请先登录'); return }
  try {
    if (followMap.value[user.id]) { ElMessage.info('已关注'); return }
    await followUser(user.id)
    followMap.value[user.id] = true
    ElMessage.success('关注成功')
  } catch(e) { ElMessage.error('操作失败') }
}

const goToMessage = (user) => {
  router.push({ path: '/messages', query: { to: user.id } })
}

const handleTabChange = () => {}

const searchByTag = (tag) => {
  clearTimeout(clickTimer)
  keyword.value = tag
  handleSearch()
}

const handleDeleteHistory = async (item) => {
  try {
    await deleteSearchHistory(item)
    searchHistory.value = searchHistory.value.filter(h => h !== item)
  } catch (e) {}
}

const handleClearHistory = async () => {
  try {
    await clearSearchHistory()
    searchHistory.value = []
  } catch (e) {}
}

const playSong = (song) => {
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

const getGradient = (index) => {
  return gradients[index % gradients.length]
}
</script>

<style scoped>
.search-page {
  padding: 0 0 40px;
  max-width: 1000px;
  margin: 0 auto;
}

.search-input-section {
  position: relative;
  margin-bottom: 24px;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  box-shadow: none !important;
  padding: 8px 16px;
  background: var(--bg-elevated) !important;
  border: 1px solid var(--border-color) !important;
  transition: all 0.3s !important;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: var(--border-glow) !important;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color) !important;
  box-shadow: var(--neon-glow-sm) !important;
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 24px;
  height: 100%;
  font-size: 15px;
  color: #fff;
  background: var(--gradient-primary);
  border-radius: 0 16px 16px 0;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: var(--neon-glow-sm);
}

.search-btn:hover {
  box-shadow: var(--neon-glow);
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  box-shadow: var(--card-hover-shadow);
  z-index: 100;
  padding: 16px;
  margin-top: 8px;
  backdrop-filter: blur(20px);
}

.dropdown-section {
  margin-bottom: 16px;
}

.dropdown-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.section-title .el-button {
  margin-left: auto;
}

.clear-btn {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color 0.2s;
}

.clear-btn:hover {
  color: var(--primary-color);
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tags .el-tag {
  cursor: pointer;
  border-radius: 10px;
  background: var(--bg-elevated);
  border-color: var(--border-color);
  color: var(--text-secondary);
}

.hot-tags .el-tag:hover {
  border-color: var(--border-glow);
  color: var(--primary-color);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  transition: all 0.2s;
}

.history-item:hover {
  background: var(--nav-hover-bg);
}

.history-item .el-icon:first-child {
  color: var(--text-tertiary);
}

.history-item .delete-icon {
  margin-left: auto;
  color: var(--text-tertiary);
  opacity: 0;
  transition: opacity 0.2s;
}

.history-item:hover .delete-icon {
  opacity: 1;
}

.search-results {
  background: var(--bg-secondary);
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
}

.song-list {
  background: var(--bg-secondary);
  border-radius: 14px;
  overflow: hidden;
}

.song-list-header {
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
  transition: color 0.2s;
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
  transition: color 0.2s;
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
  transition: color 0.2s;
}

.col-album:hover {
  color: var(--primary-color);
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

.playlist-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.playlist-card {
  background: var(--bg-secondary);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--border-color);
}

.playlist-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--card-hover-shadow);
  border-color: var(--border-glow);
}

.playlist-cover {
  position: relative;
  width: 100%;
  padding-top: 100%;
}

.playlist-cover img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8);
}

.playlist-info {
  padding: 12px;
}

.playlist-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.user-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  border-radius: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  transition: all 0.2s;
  cursor: default;
}

.user-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--card-hover-shadow);
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #a855f7);
}

.user-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-signature {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.singer-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.singer-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.singer-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--card-hover-shadow);
  transform: translateY(-2px);
}

.singer-card .singer-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.singer-card .singer-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.singer-card .singer-avatar .avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  color: #fff;
}

.singer-card .singer-info {
  min-width: 0;
}

.singer-card .singer-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.singer-card .singer-count {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.post-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  padding: 16px 18px;
  border-radius: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.post-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--card-hover-shadow);
}

.post-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.post-card-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.post-card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.post-card-content {
  font-size: 13px;
  color: var(--text-tertiary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-card-stats {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.post-card-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
