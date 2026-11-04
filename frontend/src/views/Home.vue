<template>
  <div class="home" :style="themeStyle">
    <section class="banner-section">
      <el-carousel height="280px" :interval="5000" arrow="hover" indicator-position="outside">
        <el-carousel-item v-for="(banner, idx) in banners" :key="idx">
          <div class="banner-slide" :style="{ background: banner.bg }">
            <div class="banner-content">
              <div class="banner-tag">{{ banner.tag }}</div>
              <h2 class="banner-title">{{ banner.title }}</h2>
              <p class="banner-desc">{{ banner.desc }}</p>
              <div class="banner-action" @click="banner.action">{{ banner.btn }}</div>
            </div>
            <div class="banner-visual">
              <div class="banner-disc">
                <img v-if="banner.cover" :src="banner.cover" />
                <el-icon v-else :size="60"><Music /></el-icon>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="section" v-if="userStore.isLoggedIn()">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><StarFilled /></el-icon> 每日推荐
        </h2>
        <div class="view-all-btn" @click="$router.push('/daily-recommend')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
      <div class="song-grid">
        <div
          v-for="(song, index) in dailySongs.slice(0, 6)"
          :key="song.id"
          class="song-card"
          @click="playSong(song)"
        >
          <div class="card-cover">
            <img v-if="song.coverUrl" :src="song.coverUrl" />
            <div v-else class="cover-placeholder" :style="{ background: getGradient(index) }">
              <el-icon :size="50"><Music /></el-icon>
            </div>
            <div class="play-overlay">
              <el-icon :size="40" color="#fff"><VideoPlay /></el-icon>
            </div>
          </div>
          <div class="card-info">
            <div class="card-name">{{ song.name }}</div>
            <div class="card-singer">{{ song.singer }}</div>
          </div>
        </div>
      </div>
      <el-empty v-if="!dailySongs.length" description="暂无推荐" />
    </section>

    <section class="section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><Document /></el-icon> 推荐歌单
        </h2>
        <div class="view-all-btn" @click="$router.push('/playlists')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
      <div class="playlist-scroll">
        <div
          v-for="(pl, index) in recommendPlaylists.slice(0, 8)"
          :key="pl.id"
          class="playlist-card"
          @click="$router.push(`/playlist/${pl.id}`)"
        >
          <div class="playlist-cover">
            <img v-if="pl.coverUrl" :src="pl.coverUrl" />
            <div v-else class="cover-placeholder" :style="{ background: getGradient(index) }">
              <el-icon :size="40"><Music /></el-icon>
            </div>
            <div class="play-count">
              <el-icon><Headset /></el-icon> {{ formatCount(pl.playCount || 0) }}
            </div>
          </div>
          <div class="playlist-name">{{ pl.name }}</div>
        </div>
      </div>
      <el-empty v-if="!recommendPlaylists.length" description="暂无推荐歌单" />
    </section>

    <section class="section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><Music /></el-icon> 全部歌曲
        </h2>
      </div>
      <div class="songs-table-wrapper">
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
          :total="allSongs.length"
          layout="prev, pager, next"
          background
          small
        />
      </div>
    </section>

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
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getAllSongs, getUserPlaylists, addSongToPlaylist, createPlaylist, getPublicPlaylists, fillMissingDurations } from '@/api/song'
import { getDailyRecommendations } from '@/api/recommend'
import { toggleFavorite, checkFavorite } from '@/api/favorite'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const store = useMusicStore()
const userStore = useUserStore()
const dailySongs = ref([])
const allSongs = ref([])
const recommendPlaylists = ref([])
const myPlaylists = ref([])
const showCreateDialog = ref(false)
const creating = ref(false)
const pendingSongId = ref(null)
const favoriteMap = ref({})
const currentPage = ref(1)
const pageSize = 12
const dominantColor = ref(null)

const bannerBgs = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
]

const banners = computed(() => {
  const list = []
  const themeBg = dominantColor.value
    ? `linear-gradient(135deg, rgba(${dominantColor.value.r}, ${dominantColor.value.g}, ${dominantColor.value.b}, 0.9) 0%, rgba(${Math.min(255, dominantColor.value.r + 50)}, ${Math.min(255, dominantColor.value.g + 50)}, ${Math.min(255, dominantColor.value.b + 50)}, 0.7) 100%)`
    : null
  if (dailySongs.value.length) {
    const s = dailySongs.value[0]
    list.push({
      bg: themeBg || bannerBgs[0],
      tag: '每日推荐',
      title: s.name,
      desc: `${s.singer} — 今日为你精选`,
      btn: '立即播放',
      cover: s.coverUrl,
      action: () => playSong(s)
    })
  }
  if (recommendPlaylists.value.length) {
    const p = recommendPlaylists.value[0]
    list.push({
      bg: bannerBgs[2],
      tag: '精选歌单',
      title: p.name,
      desc: p.description || '发现更多好音乐',
      btn: '查看歌单',
      cover: p.coverUrl,
      action: () => router.push(`/playlist/${p.id}`)
    })
  }
  if (list.length === 0) {
    list.push({
      bg: bannerBgs[3],
      tag: '欢迎',
      title: '发现好音乐',
      desc: '聆听属于你的音乐世界',
      btn: '开始探索',
      action: () => {}
    })
  }
  return list
})

const paginatedSongs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return allSongs.value.slice(start, start + pageSize)
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
  try {
    const plRes = await getPublicPlaylists()
    recommendPlaylists.value = plRes.data || []
  } catch (e) {}

  if (userStore.isLoggedIn()) {
    try {
      const dailyRes = await getDailyRecommendations(10)
      dailySongs.value = dailyRes.data || []
    } catch (e) {}
  }

  const allRes = await getAllSongs()
  allSongs.value = allRes.data || []
  store.setPlayList(allSongs.value)
  fillMissingDurations(allSongs.value)

  if (userStore.isLoggedIn() && userStore.userInfo?.userId) {
    const plRes = await getUserPlaylists(userStore.userInfo.userId)
    myPlaylists.value = plRes.data || []
    await loadFavorites()
  }

})

const themeStyle = computed(() => {
  if (!dominantColor.value) return {}
  const c = dominantColor.value
  return {
    '--theme-r': c.r,
    '--theme-g': c.g,
    '--theme-b': c.b,
    '--theme-color': `rgb(${c.r}, ${c.g}, ${c.b})`,
    '--theme-color-light': `rgba(${c.r}, ${c.g}, ${c.b}, 0.15)`,
    '--theme-color-medium': `rgba(${c.r}, ${c.g}, ${c.b}, 0.3)`,
    '--theme-gradient': `linear-gradient(135deg, rgba(${c.r}, ${c.g}, ${c.b}, 0.8) 0%, rgba(${Math.min(255, c.r + 40)}, ${Math.min(255, c.g + 40)}, ${Math.min(255, c.b + 40)}, 0.6) 100%)`
  }
})

const extractDominantColor = (imageUrl) => {
  if (!imageUrl) return
  const img = new Image()
  img.crossOrigin = 'Anonymous'
  img.onload = () => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    canvas.width = 50
    canvas.height = 50
    ctx.drawImage(img, 0, 0, 50, 50)
    const data = ctx.getImageData(0, 0, 50, 50).data
    let r = 0, g = 0, b = 0, count = 0
    for (let i = 0; i < data.length; i += 16) {
      const pr = data[i], pg = data[i + 1], pb = data[i + 2]
      if (pr + pg + pb < 100 || pr + pg + pb > 650) continue
      r += pr; g += pg; b += pb; count++
    }
    if (count > 0) {
      r = Math.round(r / count)
      g = Math.round(g / count)
      b = Math.round(b / count)
      const max = Math.max(r, g, b)
      const min = Math.min(r, g, b)
      if (max - min < 30) {
        r = Math.min(255, r + 60)
        g = Math.min(255, g + 30)
      }
      dominantColor.value = { r, g, b }
    }
  }
  img.onerror = () => { dominantColor.value = null }
  img.src = imageUrl
}

watch(() => store.currentSong, (song) => {
  if (song?.coverUrl) {
    extractDominantColor(song.coverUrl)
  } else {
    dominantColor.value = null
  }
}, { immediate: true })

const loadFavorites = async () => {
  try {
    for (const song of allSongs.value) {
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

const playSong = (song) => {
  store.setCurrentSong(song)
}

const handleAddToQueue = (song) => {
  const added = store.addToQueue(song)
  if (added) {
    store.isQueueMode = true
    ElMessage.success('已添加到播放列表')
  } else {
    ElMessage.info('歌曲已在播放列表中')
  }
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

const formatCount = (count) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count.toString()
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
.home {
  padding-bottom: 20px;
}

.banner-section {
  margin-bottom: 40px;
}

.banner-section :deep(.el-carousel__container) {
  border-radius: 20px;
}

.banner-section :deep(.el-carousel__indicators--outside) {
  margin-top: 12px;
}

.banner-section :deep(.el-carousel__indicator .el-carousel__button) {
  width: 24px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255,255,255,0.2);
}

.banner-section :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  background: var(--primary-color);
  width: 32px;
}

.banner-slide {
  height: 280px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 60px;
  position: relative;
  overflow: hidden;
}

.banner-content {
  z-index: 1;
  max-width: 55%;
}

.banner-tag {
  display: inline-block;
  padding: 4px 14px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  font-size: 12px;
  color: #fff;
  margin-bottom: 16px;
  font-weight: 500;
}

.banner-title {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.banner-desc {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  margin: 0 0 24px;
}

.banner-action {
  display: inline-flex;
  align-items: center;
  padding: 10px 28px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 30px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.banner-action:hover {
  background: rgba(255,255,255,0.35);
  transform: scale(1.05);
}

.banner-visual {
  z-index: 1;
}

.banner-disc {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  overflow: hidden;
  border: 6px solid rgba(255,255,255,0.15);
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.2);
  color: rgba(255,255,255,0.6);
}

.banner-disc img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.section {
  margin-bottom: 48px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title :deep(.el-icon) {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.playlist-scroll {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 20px;
}

.playlist-card {
  cursor: pointer;
  transition: all 0.3s;
}

.playlist-card:hover {
  transform: translateY(-6px);
}

.playlist-cover {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 14px;
  box-shadow: var(--card-shadow);
  transition: all 0.3s;
  border: 1px solid var(--border-color);
}

.playlist-card:hover .playlist-cover {
  box-shadow: var(--card-hover-shadow);
  border-color: var(--border-glow);
}

.playlist-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.playlist-cover .cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8);
}

.play-count {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  background: rgba(7, 7, 14, 0.7);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  font-size: 11px;
  color: #fff;
}

.playlist-name {
  padding: 10px 4px 0;
  font-size: 13px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.song-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 24px;
}

.song-card {
  cursor: pointer;
  transition: all 0.3s;
}

.song-card:hover {
  transform: translateY(-6px);
}

.card-cover {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 14px;
  box-shadow: var(--card-shadow);
  transition: all 0.3s;
  border: 1px solid var(--border-color);
}

.song-card:hover .card-cover {
  box-shadow: var(--card-hover-shadow);
  border-color: var(--border-glow);
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(7, 7, 14, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.song-card:hover .play-overlay {
  opacity: 1;
}

.card-info {
  padding: 12px 4px 0;
}

.card-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.card-singer {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.songs-table-wrapper {
  background: var(--bg-secondary);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
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

.song-row.active .col-index {
  color: var(--primary-color);
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
  transition: all 0.2s;
}

.col-action .action-btn:hover {
  background: var(--nav-hover-bg);
  color: var(--primary-color);
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.view-all-btn:hover {
  color: var(--primary-color);
  border-color: var(--border-glow);
  background: var(--nav-hover-bg);
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

.community-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.community-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.community-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--theme-color, var(--gradient-primary));
  opacity: 0;
  transition: opacity 0.3s;
}

.community-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-hover-shadow);
  border-color: var(--border-glow);
}

.community-card:hover::after {
  opacity: 1;
}

.cc-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.cc-user {
  overflow: hidden;
}

.cc-nickname {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cc-time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.cc-body {
  margin-bottom: 14px;
}

.cc-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cc-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cc-image {
  width: 100%;
  max-height: 120px;
  object-fit: cover;
  border-radius: 10px;
  margin-top: 10px;
  border: 1px solid var(--border-color);
}

.cc-footer {
  display: flex;
  gap: 16px;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.cc-stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.banner-section {
  transition: all 0.6s ease;
}

.banner-slide {
  transition: background 0.6s ease;
}

.section-title :deep(.el-icon) {
  transition: all 0.3s;
}
</style>
