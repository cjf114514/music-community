<template>
  <div class="playlist-detail" v-loading="loading">
    <NotFound v-if="notFound" title="歌单不存在或已被删除" subtitle="分享链接可能已失效，或者这个歌单已经被作者删除" />
    <div v-if="playlist" class="playlist-header">
      <div class="playlist-cover-large">
        <el-image v-if="playlist.coverUrl" :src="playlist.coverUrl" fit="cover" />
        <div v-else class="cover-placeholder-large">
          <el-icon :size="60"><Music /></el-icon>
        </div>
      </div>
      <div class="playlist-meta">
        <h1 class="playlist-title">{{ playlist.name }}</h1>
        <p class="playlist-desc">{{ playlist.description || '暂无描述' }}</p>
        <div class="playlist-actions">
          <el-button type="primary" size="large" @click="playAll">
            <el-icon><VideoPlay /></el-icon> 播放全部
          </el-button>
          <el-button v-if="userStore.isLoggedIn()" size="large" @click="handleShare">
            <el-icon><Share /></el-icon> 分享
          </el-button>
          <el-button v-if="isOwner" size="large" @click="showEditDialog = true">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button v-if="isOwner" type="danger" size="large" @click="handleDelete">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="songs.length" class="song-list">
      <div class="song-list-header">
        <span class="col-index">#</span>
        <span class="col-title">标题</span>
        <span class="col-album">专辑</span>
        <span v-if="userStore.isLoggedIn()" class="col-like">喜欢</span>
        <span class="col-duration">时长</span>
        <span class="col-action">操作</span>
      </div>
      <draggable
        v-model="songs"
        :group="{ name: 'playlist-songs', pull: false, put: false }"
        item-key="id"
        :animation="200"
        ghost-class="ghost"
        :disabled="!isOwner"
        @end="onDragEnd"
        tag="div"
        class="song-list-body"
      >
        <template #item="{ element: song, index }">
          <div
            class="song-item"
            :class="{ active: store.currentSong?.id === song.id }"
            @dblclick="playSong(song, index)"
          >
            <span class="col-index">
              <span v-if="isOwner" class="drag-handle">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>
              </span>
              <span v-else class="index-num">{{ index + 1 }}</span>
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
          <div v-if="isOwner" class="action-btn" @click="handleRemove(song.id)">
            <el-icon><Delete /></el-icon>
          </div>
        </span>
      </div>
        </template>
      </draggable>
    </div>

    <el-empty v-else description="歌单暂无歌曲" />

    <div class="comments-section">
      <h3 class="section-title">评论 ({{ totalCount }})</h3>
      <div v-if="userStore.isLoggedIn()" class="comment-input-area">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          maxlength="500"
          show-word-limit
        />
        <div class="comment-actions">
          <el-button type="primary" @click="handleAddComment" :disabled="!newComment.trim()">
            发表评论
          </el-button>
        </div>
      </div>
      <div v-else class="guest-login-hint">
        <span>登录后即可发表评论</span>
        <el-button type="primary" size="small" @click="$router.push('/login')">去登录</el-button>
      </div>
      <div class="sort-tabs">
        <span
          class="sort-tab"
          :class="{ active: sortBy === 'time' }"
          @click="sortBy = 'time'; loadComments()"
        >
          最新
        </span>
        <span
          class="sort-tab"
          :class="{ active: sortBy === 'hot' }"
          @click="sortBy = 'hot'; loadComments()"
        >
          最热
        </span>
      </div>
      <div v-if="comments.length" class="comments-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <el-avatar :size="36" :src="comment.avatar" />
            <div class="comment-info">
              <div class="comment-user">{{ comment.nickname || comment.username }}</div>
              <div class="comment-time">{{ formatTime(comment.createTime) }}</div>
            </div>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
          <div class="comment-footer">
            <div v-if="userStore.isLoggedIn()" class="comment-action-btn" @click="handleLike(comment)">
              <el-icon :class="{ liked: comment.liked }">
                <StarFilled v-if="comment.liked" />
                <Star v-else />
              </el-icon>
              <span>{{ comment.likeCount || '' }}</span>
            </div>
            <div v-else class="comment-action-btn disabled-action">
              <el-icon><Star /></el-icon>
              <span>{{ comment.likeCount || '' }}</span>
            </div>
            <div v-if="userStore.isLoggedIn()" class="comment-action-btn" @click="replyingTo = comment">
              <el-icon><ChatLineRound /></el-icon>
              <span>回复</span>
            </div>
          </div>
          <div v-if="comment.replies && comment.replies.length" class="replies">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <div class="reply-header">
                <el-avatar :size="24" :src="reply.avatar" />
                <div class="reply-info">
                  <span class="reply-user">{{ reply.nickname || reply.username }}</span>
                  <span v-if="reply.replyToNickname" class="reply-to">回复 {{ reply.replyToNickname }}</span>
                  <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                </div>
              </div>
              <div class="reply-content">{{ reply.content }}</div>
              <div class="reply-footer">
                <div v-if="userStore.isLoggedIn()" class="comment-action-btn" @click="handleLike(reply)">
                  <el-icon :class="{ liked: reply.liked }">
                    <StarFilled v-if="reply.liked" />
                    <Star v-else />
                  </el-icon>
                  <span>{{ reply.likeCount || '' }}</span>
                </div>
                <div v-else class="comment-action-btn disabled-action">
                  <el-icon><Star /></el-icon>
                  <span>{{ reply.likeCount || '' }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="replyingTo?.id === comment.id" class="reply-input-area">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="2"
              :placeholder="`回复 ${comment.nickname || comment.username}：`"
              maxlength="500"
            />
            <div class="reply-actions">
              <el-button size="small" @click="replyingTo = null">取消</el-button>
              <el-button size="small" type="primary" @click="handleReply(comment.id)" :disabled="!replyContent.trim()">
                回复
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无评论，快来抢沙发吧" />
    </div>

    <el-dialog v-model="showEditDialog" title="编辑歌单" width="500px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="歌单封面">
          <el-upload
            ref="coverUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".jpg,.jpeg,.png,.gif,.webp"
            :on-change="handleCoverChange"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div v-if="editForm.coverUrl && !coverFile" class="current-cover-hint">
            当前封面: <el-image :src="editForm.coverUrl" style="width: 40px; height: 40px; vertical-align: middle; border-radius: 4px" fit="cover" />
          </div>
        </el-form-item>
        <el-form-item label="歌单名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="歌单描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="歌单分类">
          <el-select v-model="editForm.category" placeholder="选择分类，也可直接输入自定义" clearable filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="c in PLAYLIST_CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <ShareDialog
      v-model="shareVisible"
      type="playlist"
      :id="playlist?.id"
      :title="playlist?.name"
      :subtitle="playlist?.description || '暂无描述'"
      :cover-url="playlist?.coverUrl"
      :share-token="shareToken"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPlaylistById, getPlaylistSongs, updatePlaylist, deletePlaylist, removeSongFromPlaylist, uploadCover, fillMissingDurations, reorderPlaylistSongs } from '@/api/song'
import { toggleFavorite, checkFavorite } from '@/api/favorite'
import { getPlaylistComments, addPlaylistComment, deleteComment, toggleLike } from '@/api/comment'
import { sharePlaylist } from '@/api/social'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { usePlaylistStore } from '@/stores/playlist'
import { ElMessage, ElMessageBox } from 'element-plus'
import draggable from 'vuedraggable'
import ShareDialog from '@/components/ShareDialog.vue'
import NotFound from '@/components/NotFound.vue'
import { PLAYLIST_CATEGORIES } from '@/constants/playlist'

const route = useRoute()
const router = useRouter()
const store = useMusicStore()
const userStore = useUserStore()
const playlistStore = usePlaylistStore()

const playlist = ref(null)
const songs = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const saving = ref(false)
const coverFile = ref(null)
const favoriteMap = ref({})
const currentPage = ref(1)
const pageSize = 12

const paginatedSongs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return songs.value.slice(start, start + pageSize)
})

const comments = ref([])
const totalCount = ref(0)
const newComment = ref('')
const replyContent = ref('')
const replyingTo = ref(null)
const sortBy = ref('time')
const shareVisible = ref(false)
const shareToken = ref('')

const editForm = ref({ name: '', description: '', coverUrl: '', category: '' })
const notFound = ref(false)

const isOwner = computed(() => {
  return playlist.value?.userId === userStore.userInfo?.userId
})

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  loading.value = true
  currentPage.value = 1
  try {
    const plRes = await getPlaylistById(route.params.id)
    playlist.value = plRes.data
    songs.value = []
    if (playlist.value) {
      editForm.value = { name: playlist.value.name, description: playlist.value.description, coverUrl: playlist.value.coverUrl, category: playlist.value.category || '' }
    }
  } catch (e) {
    if (e?.response?.status === 404) {
      notFound.value = true          // 歌单被删 / 分享链接失效
    } else {
      ElMessage.error(e.response?.data?.message || '加载歌单失败')
    }
    return
  } finally {
    loading.value = false
  }

  try {
    const songsRes = await getPlaylistSongs(route.params.id)
    songs.value = songsRes.data || []
    fillMissingDurations(songs.value)
    if (userStore.isLoggedIn()) {
      await loadFavorites()
    }
  } catch (e) {
    ElMessage.error('加载歌曲列表失败')
    songs.value = []
  }

  await loadComments()
}

const loadComments = async () => {
  try {
    const res = await getPlaylistComments(route.params.id, sortBy.value)
    comments.value = res.data.comments || []
    totalCount.value = res.data.totalCount || 0
  } catch (e) {}
}

const handleAddComment = async () => {
  if (!newComment.value.trim()) return
  try {
    await addPlaylistComment(route.params.id, { content: newComment.value })
    ElMessage.success('评论成功')
    newComment.value = ''
    await loadComments()
  } catch (e) {
    ElMessage.error('评论失败')
  }
}

const handleReply = async (parentId) => {
  if (!replyContent.value.trim()) return
  try {
    await addPlaylistComment(route.params.id, { content: replyContent.value, parentId })
    ElMessage.success('回复成功')
    replyContent.value = ''
    replyingTo.value = null
    await loadComments()
  } catch (e) {
    ElMessage.error('回复失败')
  }
}

const handleLike = async (comment) => {
  try {
    const res = await toggleLike(comment.id)
    const isNowLiked = res.data
    comment.liked = isNowLiked
    comment.likeCount = isNowLiked ? (comment.likeCount || 0) + 1 : Math.max(0, (comment.likeCount || 0) - 1)
  } catch (e) {
    ElMessage.error('点赞失败')
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 2592000000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString()
}

const loadFavorites = async () => {
  try {
    for (const song of songs.value) {
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

const formatDuration = (seconds) => {
  if (!seconds) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

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

const handleCoverChange = (file) => {
  coverFile.value = file.raw
}

const handleSave = async () => {
  saving.value = true
  try {
    let coverUrl = editForm.value.coverUrl
    if (coverFile.value) {
      const coverRes = await uploadCover(coverFile.value)
      coverUrl = coverRes.data
    }
    await updatePlaylist(playlist.value.id, {
      name: editForm.value.name,
      description: editForm.value.description,
      coverUrl,
      category: editForm.value.category || undefined
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    coverFile.value = null
    await loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这个歌单吗？', '提示', { type: 'warning' })
    await deletePlaylist(playlist.value.id)
    playlistStore.removePlaylist(playlist.value.id)
    ElMessage.success('删除成功')
    router.push('/playlists')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleShare = async () => {
  try {
    const res = await sharePlaylist(playlist.value.id)
    shareToken.value = res.data.shareToken
    shareVisible.value = true
  } catch (e) {
    ElMessage.error('分享失败')
  }
}

const handleRemove = async (songId) => {
  try {
    await removeSongFromPlaylist(playlist.value.id, songId)
    ElMessage.success('已移除')
    songs.value = songs.value.filter(s => s.id !== songId)
  } catch (e) {
    ElMessage.error('移除失败')
  }
}

const onDragEnd = async () => {
  if (!isOwner.value) return
  try {
    const songIds = songs.value.map(s => s.id)
    await reorderPlaylistSongs(playlist.value.id, songIds)
  } catch (e) {
    ElMessage.error('排序保存失败')
    const songsRes = await getPlaylistSongs(route.params.id)
    songs.value = songsRes.data || []
  }
}
</script>

<style scoped>
.playlist-detail {
  padding: 20px 0;
}

.playlist-header {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
}

.playlist-cover-large {
  width: 200px;
  height: 200px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
  box-shadow: var(--card-shadow);
}

.playlist-cover-large .el-image {
  width: 100%;
  height: 100%;
}

.cover-placeholder-large {
  width: 100%;
  height: 100%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
}

.playlist-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.playlist-title {
  font-size: 28px;
  color: var(--text-primary);
  margin: 0 0 8px;
  font-weight: 700;
}

.playlist-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 20px;
}

.playlist-actions {
  display: flex;
  gap: 12px;
}

.song-list {
  background: var(--bg-secondary);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.song-list-header {
  display: grid;
  grid-template-columns: 50px 1fr 200px 60px 80px 100px;
  padding: 14px 20px;
  background: var(--bg-elevated);
  font-size: 12px;
  color: var(--text-tertiary);
  border-bottom: 1px solid var(--border-color);
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
}

.song-item {
  display: grid;
  grid-template-columns: 50px 1fr 200px 60px 80px 100px;
  padding: 10px 20px;
  align-items: center;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid rgba(99, 102, 241, 0.04);
}

.song-item:hover {
  background: var(--nav-hover-bg);
}

.song-item.active {
  color: var(--primary-color);
  background: var(--nav-active-bg);
}

.col-index {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}

.drag-handle {
  cursor: grab;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.song-item:hover .drag-handle {
  opacity: 1;
}

.drag-handle:active {
  cursor: grabbing;
}

.ghost {
  opacity: 0.4;
  background: var(--nav-active-bg);
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

.current-cover-hint {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.comments-section {
  margin-top: 40px;
  background: var(--bg-secondary);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.section-title {
  font-size: 18px;
  color: var(--text-primary);
  margin: 0 0 20px;
  font-weight: 700;
}

.comment-input-area {
  margin-bottom: 20px;
}

.comment-input-area :deep(.el-textarea__inner) {
  background: var(--bg-elevated);
  border-color: var(--border-color);
  color: var(--text-primary);
}

.comment-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.sort-tabs {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 8px;
}

.sort-tab {
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.sort-tab:hover {
  color: var(--text-primary);
}

.sort-tab.active {
  color: var(--primary-color);
  font-weight: 500;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-info {
  flex: 1;
}

.comment-user {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.comment-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.comment-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: 10px;
}

.comment-footer {
  display: flex;
  gap: 20px;
}

.comment-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.comment-action-btn:hover {
  color: var(--primary-color);
}

.comment-action-btn .liked {
  color: var(--accent-pink);
}

.replies {
  margin-top: 12px;
  background: var(--bg-elevated);
  border-radius: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
}

.reply-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}

.reply-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.reply-info {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.reply-user {
  color: var(--text-primary);
  font-weight: 500;
}

.reply-to {
  color: var(--text-secondary);
}

.reply-time {
  color: var(--text-tertiary);
}

.reply-content {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 4px;
}

.reply-footer {
  display: flex;
  gap: 16px;
}

.reply-input-area {
  margin-top: 12px;
}

.reply-input-area :deep(.el-textarea__inner) {
  background: var(--bg-elevated);
  border-color: var(--border-color);
  color: var(--text-primary);
}

.reply-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.guest-login-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--bg-secondary);
  border-radius: 12px;
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 16px;
}

.disabled-action {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
