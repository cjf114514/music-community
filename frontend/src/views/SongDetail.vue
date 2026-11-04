<template>
  <div class="song-detail" v-loading="loading">
    <NotFound v-if="notFound" title="歌曲不存在或已下架" subtitle="这首歌可能已被删除，或者链接已经失效" />
    <div v-if="song" class="song-header">
      <div class="song-cover-large">
        <el-image v-if="song.coverUrl" :src="song.coverUrl" fit="cover" />
        <div v-else class="cover-placeholder-large">
          <svg viewBox="0 0 24 24" fill="currentColor" width="60" height="60"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
        </div>
      </div>
      <div class="song-meta">
        <h1 class="song-title">{{ song.name }}</h1>
        <p class="song-singer">{{ song.singer }} - {{ song.album }}</p>
        <div class="song-actions">
          <el-button type="primary" size="large" @click="playSong">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M8 5v14l11-7z"/></svg>
            播放
          </el-button>
          <template v-if="userStore.isLoggedIn()">
            <el-button size="large" @click="handleToggleFavorite">
              <svg v-if="isFavorite" viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z"/></svg>
              {{ isFavorite ? '已收藏' : '收藏' }}
            </el-button>
            <el-button size="large" @click="addToPlaylistVisible = true">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
              添加到歌单
            </el-button>
            <el-button size="large" @click="handleDownload">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
              下载
            </el-button>
            <el-button size="large" @click="handleShare">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"/></svg>
              分享
            </el-button>
          </template>
        </div>
      </div>
    </div>

    <div class="content-tabs">
      <div
        :class="['tab-item', { active: activeTab === 'lyrics' }]"
        @click="activeTab = 'lyrics'"
      >歌词</div>
      <div
        :class="['tab-item', { active: activeTab === 'comments' }]"
        @click="activeTab = 'comments'"
      >评论 ({{ totalCount }})</div>
      <div
        :class="['tab-item', { active: activeTab === 'similar' }]"
        @click="activeTab = 'similar'"
      >相似推荐</div>
    </div>

    <div v-if="activeTab === 'lyrics'" class="lyrics-section">
      <div class="lyrics-toolbar">
        <div class="lyric-offset-control">
          <span class="offset-label">偏移</span>
          <el-button size="small" text @click="adjustOffset(-0.5)" :disabled="lyricOffset <= -5">-</el-button>
          <span class="offset-value">{{ lyricOffset > 0 ? '+' : '' }}{{ lyricOffset.toFixed(1) }}s</span>
          <el-button size="small" text @click="adjustOffset(0.5)" :disabled="lyricOffset >= 5">+</el-button>
          <el-button size="small" text v-if="lyricOffset !== 0" @click="adjustOffset(-lyricOffset)">重置</el-button>
        </div>
        <div class="lyrics-toolbar-right">
          <span class="lyrics-count" v-if="parsedLyrics.length">{{ parsedLyrics.length }} 行</span>
          <el-switch
            v-model="showDesktopLyrics"
            size="small"
            inline-prompt
            active-text="桌面歌词"
            inactive-text=""
            :active-value="true"
            :inactive-value="false"
            @change="handleDesktopLyricsToggle"
          />
        </div>
      </div>
      <div class="lyrics-content" ref="lyricsScrollRef">
        <div
          v-for="(line, index) in parsedLyrics"
          :key="index"
          :class="['lyric-line', {
            active: currentLyricIndex === index,
            nearby: Math.abs(currentLyricIndex - index) === 1,
            dim: currentLyricIndex >= 0 && Math.abs(currentLyricIndex - index) > 1
          }]"
          @click="seekToLine(line.time)"
        >
          <span class="lyric-text">{{ line.text }}</span>
          <span class="lyric-translation" v-if="line.translation">{{ line.translation }}</span>
        </div>
        <div v-if="!parsedLyrics.length && !loadingLyric" class="lyric-empty">暂无歌词</div>
        <div v-if="loadingLyric" class="lyric-loading">加载歌词中...</div>
      </div>
    </div>

    <div v-if="activeTab === 'comments'" class="comment-section">
      <div v-if="userStore.isLoggedIn()" class="comment-input">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          maxlength="500"
          show-word-limit
        />
        <div class="comment-input-actions">
          <el-button type="primary" :disabled="!newComment.trim()" @click="handlePostComment">发表评论</el-button>
        </div>
      </div>
      <div v-else class="guest-login-hint">
        <span>登录后即可发表评论</span>
        <el-button type="primary" size="small" @click="$router.push('/login')">去登录</el-button>
      </div>

      <div class="comment-sort">
        <span :class="['sort-tab', { active: sortBy === 'time' }]" @click="switchSort('time')">最新</span>
        <span :class="['sort-tab', { active: sortBy === 'hot' }]" @click="switchSort('hot')">最热</span>
      </div>

      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-main">
          <el-avatar :size="36" :src="comment.avatar">
            {{ comment.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-nickname">{{ comment.nickname }}</span>
              <span v-if="comment.timestamp" class="comment-timestamp" @click="seekToTime(comment.timestamp)">
                <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
                {{ formatTimestamp(comment.timestamp) }}
              </span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-footer">
              <span v-if="userStore.isLoggedIn()" class="comment-action" :class="{ liked: comment.liked }" @click="handleLike(comment)">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
                {{ comment.likeCount || 0 }}
              </span>
              <span v-else class="comment-action disabled-like">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
                {{ comment.likeCount || 0 }}
              </span>
              <span v-if="userStore.isLoggedIn()" class="comment-action" @click="openReply(comment)">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/></svg>
                回复
              </span>
              <span v-if="isOwner(comment)" class="comment-action delete" @click="handleDelete(comment.id)">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
                删除
              </span>
            </div>
          </div>
        </div>

        <div v-if="comment.replies?.length" class="comment-replies">
          <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
            <el-avatar :size="28" :src="reply.avatar">
              {{ reply.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="reply-body">
              <div class="reply-header">
                <span class="reply-nickname">{{ reply.nickname }}</span>
                <span v-if="reply.replyToNickname" class="reply-to">
                  回复 <span class="reply-to-name">{{ reply.replyToNickname }}</span>
                </span>
                <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
              </div>
              <div class="reply-content">{{ reply.content }}</div>
              <div class="reply-footer">
                <span v-if="userStore.isLoggedIn()" class="comment-action" :class="{ liked: reply.liked }" @click="handleLike(reply)">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
                  {{ reply.likeCount || 0 }}
                </span>
                <span v-else class="comment-action disabled-like">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
                  {{ reply.likeCount || 0 }}
                </span>
                <span v-if="userStore.isLoggedIn()" class="comment-action" @click="openReply(comment, reply)">回复</span>
                <span v-if="isOwner(reply)" class="comment-action delete" @click="handleDelete(reply.id)">删除</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="replyingTo?.parentId === comment.id || replyingTo?.id === comment.id" class="reply-input">
          <el-input v-model="replyContent" type="textarea" :rows="2" :placeholder="replyPlaceholder" maxlength="500" />
          <div class="reply-input-actions">
            <el-button size="small" @click="replyingTo = null">取消</el-button>
            <el-button type="primary" size="small" :disabled="!replyContent.trim()" @click="handlePostReply(comment.id)">回复</el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!comments.length" description="暂无评论，快来抢沙发吧" />
    </div>

    <div v-if="activeTab === 'similar'" class="similar-section">
      <div v-if="similarLoading" class="similar-loading">加载中...</div>
      <div v-else-if="similarSongs.length" class="similar-grid">
        <div
          v-for="(s, idx) in similarSongs"
          :key="s.id"
          class="similar-card"
          @click="$router.push(`/song/${s.id}`)"
        >
          <div class="similar-cover">
            <img v-if="s.coverUrl" :src="s.coverUrl" />
            <div v-else class="cover-ph" :style="{ background: getGradient(idx) }">
              <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
            </div>
            <div class="similar-play" @click.stop="playSimilarSong(s)">
              <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M8 5v14l11-7z"/></svg>
            </div>
            <div class="similar-add" @click.stop="handleAddToQueue(s)" title="添加到播放列表">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
            </div>
          </div>
          <div class="similar-info">
            <div class="similar-name">{{ s.name }}</div>
            <div class="similar-singer">{{ s.singer }}</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无相似推荐" />
    </div>

    <el-dialog v-model="addToPlaylistVisible" title="添加到歌单" width="400px">
      <el-menu v-if="myPlaylists.length">
        <el-menu-item v-for="pl in myPlaylists" :key="pl.id" @click="handleAddToPlaylist(pl.id)">
          {{ pl.name }}
        </el-menu-item>
      </el-menu>
      <el-empty v-else description="暂无歌单" />
    </el-dialog>

    <ShareDialog
      v-model="shareVisible"
      type="song"
      :id="song?.id"
      :title="song?.name"
      :subtitle="song?.singer"
      :cover-url="song?.coverUrl"
      :share-token="shareToken"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSongById, getSongLyric, downloadSong } from '@/api/song'
import { getSongComments, addComment, deleteComment, toggleLike } from '@/api/comment'
import { getUserPlaylists, addSongToPlaylist } from '@/api/song'
import { toggleFavorite, checkFavorite } from '@/api/favorite'
import { shareSong } from '@/api/social'
import { getSimilarSongs } from '@/api/recommend'
import { useMusicStore } from '@/stores/music'
import { useUserStore } from '@/stores/user'
import { useLyric } from '@/composables/useLyric'
import { ElMessage, ElMessageBox } from 'element-plus'
import ShareDialog from '@/components/ShareDialog.vue'
import NotFound from '@/components/NotFound.vue'

const route = useRoute()
const router = useRouter()
const store = useMusicStore()
const userStore = useUserStore()

const song = ref(null)
const comments = ref([])
const totalCount = ref(0)
const loading = ref(false)
const newComment = ref('')
const replyContent = ref('')
const replyingTo = ref(null)
const addToPlaylistVisible = ref(false)
const myPlaylists = ref([])
const isFavorite = ref(false)
const lyricContent = ref('')
const loadingLyric = ref(false)
const lyricsScrollRef = ref(null)
const {
  parsedLyrics,
  currentLyricIndex,
  seekToLine,
  offset: lyricOffset,
  adjustOffset
} = useLyric(lyricContent)
const sortBy = ref('time')
const shareVisible = ref(false)
const shareToken = ref('')
const activeTab = ref('lyrics')
const similarSongs = ref([])
const similarLoading = ref(false)
const notFound = ref(false)

const getGradient = (index) => {
  const gradients = [
    'linear-gradient(135deg, #6366f1, #8b5cf6)',
    'linear-gradient(135deg, #22d3ee, #6366f1)',
    'linear-gradient(135deg, #f472b6, #8b5cf6)',
    'linear-gradient(135deg, #fbbf24, #f472b6)',
    'linear-gradient(135deg, #34d399, #22d3ee)',
    'linear-gradient(135deg, #8b5cf6, #ec4899)',
  ]
  return gradients[index % gradients.length]
}
const showDesktopLyrics = ref(store.showDesktopLyrics)

watch(() => store.showDesktopLyrics, (val) => {
  showDesktopLyrics.value = val
})

onMounted(async () => {
  await loadSong()
  await loadComments()
  if (userStore.userInfo?.userId) {
    const plRes = await getUserPlaylists(userStore.userInfo.userId)
    myPlaylists.value = plRes.data || []
    await checkFavoriteStatus()
  }
})

onBeforeUnmount(() => {
})

watch(activeTab, (tab) => {
})

const currentPlayTime = computed(() => {
  const t = Math.floor(store.currentTime || 0)
  return formatTimestamp(t)
})

const checkFavoriteStatus = async () => {
  try {
    const res = await checkFavorite(route.params.id)
    isFavorite.value = res.data
  } catch (e) {}
}

const handleToggleFavorite = async () => {
  try {
    await toggleFavorite(route.params.id)
    isFavorite.value = !isFavorite.value
    ElMessage.success(isFavorite.value ? '已收藏' : '已取消收藏')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const loadSong = async () => {
  try {
    const res = await getSongById(route.params.id)
    song.value = res.data
  } catch (e) {
    // 后端对不存在的歌曲返回 404：显示友好空状态，而不是让页面空白
    if (e?.response?.status === 404) { notFound.value = true; return }
    throw e
  }
  loadingLyric.value = true
  try {
    const lyricRes = await getSongLyric(route.params.id)
    lyricContent.value = lyricRes.data || ''
  } catch (e) {
    lyricContent.value = ''
  } finally {
    loadingLyric.value = false
  }
  loadSimilarSongs()
}

const loadSimilarSongs = async () => {
  similarLoading.value = true
  try {
    const res = await getSimilarSongs(route.params.id, 12)
    similarSongs.value = res.data || []
  } catch (e) {
    similarSongs.value = []
  } finally {
    similarLoading.value = false
  }
}

const playSimilarSong = (s) => {
  store.setPlayList([s])
  store.setCurrentSongIndex(0)
  store.play()
}

const handleAddToQueue = (song) => {
  const added = store.addToQueue(song)
  if (added) {
    ElMessage.success('已添加到播放列表')
  } else {
    ElMessage.info('歌曲已在播放列表中')
  }
}

const loadComments = async () => {
  const res = await getSongComments(route.params.id, sortBy.value)
  comments.value = res.data?.comments || []
  totalCount.value = res.data?.totalCount || 0
}

const switchSort = (type) => {
  if (sortBy.value === type) return
  sortBy.value = type
  loadComments()
}

const playSong = () => {
  store.setCurrentSong(song.value)
}

const handlePostComment = async () => {
  if (!newComment.value.trim()) return
  try {
    await addComment(route.params.id, { content: newComment.value.trim() })
    newComment.value = ''
    ElMessage.success('评论成功')
    await loadComments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '评论失败')
  }
}

const openReply = (comment, reply) => {
  replyingTo.value = reply ? { ...reply, parentId: comment.id } : { ...comment, parentId: comment.id }
  replyContent.value = ''
}

const replyPlaceholder = computed(() => {
  if (!replyingTo.value) return '写下你的回复...'
  return `回复 ${replyingTo.value.nickname || '该评论'}...`
})

const handlePostReply = async (parentId) => {
  if (!replyContent.value.trim()) return
  try {
    let content = replyContent.value.trim()
    if (replyingTo.value?.nickname && !content.startsWith('@')) {
      content = `@${replyingTo.value.nickname}：${content}`
    }
    await addComment(route.params.id, { content, parentId })
    replyContent.value = ''
    replyingTo.value = null
    ElMessage.success('回复成功')
    await loadComments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '回复失败')
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

const handleDelete = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', { type: 'warning' })
    await deleteComment(commentId)
    ElMessage.success('删除成功')
    await loadComments()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleAddToPlaylist = async (playlistId) => {
  try {
    await addSongToPlaylist(playlistId, song.value.id)
    ElMessage.success('已添加到歌单')
    addToPlaylistVisible.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '添加失败')
  }
}

const handleDownload = () => {
  if (!song.value?.id) return
  downloadSong(song.value.id)
  ElMessage.success('开始下载')
}

const handleShare = async () => {
  try {
    const res = await shareSong(song.value.id)
    shareToken.value = res.data.shareToken
    shareVisible.value = true
  } catch (e) {
    ElMessage.error('分享失败')
  }
}

const isOwner = (comment) => {
  return comment.userId === userStore.userInfo?.userId
}

const formatTimestamp = (seconds) => {
  if (seconds == null || isNaN(seconds)) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString()
}

const seekToTime = (seconds) => {
  store.seekTo(seconds)
  if (store.currentSong?.id !== song.value?.id) {
    store.setCurrentSong(song.value)
  }
}

const handleDesktopLyricsToggle = (val) => {
  store.showDesktopLyrics = val
}

watch(() => currentLyricIndex.value, (newIndex) => {
  if (newIndex >= 0 && lyricsScrollRef.value) {
    const lines = lyricsScrollRef.value.querySelectorAll('.lyric-line')
    if (lines[newIndex]) {
      lines[newIndex].scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
})
</script>

<style scoped>
.song-detail {
  padding: 20px 0;
}

.song-header {
  display: flex;
  gap: 30px;
  margin-bottom: 24px;
}

.song-cover-large {
  width: 200px;
  height: 200px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
  box-shadow: var(--card-shadow);
}

.song-cover-large .el-image {
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

.song-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.song-title {
  font-size: 28px;
  color: var(--text-primary);
  margin: 0 0 8px;
  font-weight: 700;
}

.song-singer {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 20px;
}

.song-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.song-actions .el-button {
  display: flex;
  align-items: center;
  gap: 6px;
}

.content-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0;
}

.tab-item {
  padding: 10px 20px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-item:hover {
  color: var(--text-primary);
}

.tab-item.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

.tab-badge {
  font-size: 10px;
  background: var(--gradient-primary);
  color: #fff;
  padding: 1px 6px;
  border-radius: 10px;
  line-height: 1.4;
}

.lyrics-section {
  background: var(--bg-secondary);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.lyrics-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.lyric-offset-control {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.offset-label {
  color: var(--text-tertiary);
}

.offset-value {
  min-width: 40px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}

.lyrics-toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.lyrics-count {
  font-size: 12px;
  color: var(--text-tertiary);
}

.lyrics-content {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px 0;
}

.lyrics-content::-webkit-scrollbar {
  width: 4px;
}

.lyrics-content::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 2px;
}

.lyric-line {
  padding: 8px 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 14px;
  line-height: 2;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  transform: scale(0.94);
  opacity: 0.4;
}

.lyric-line:hover {
  color: var(--text-secondary);
}

.lyric-line.nearby {
  opacity: 0.7;
  transform: scale(0.97);
}

.lyric-line.dim {
  opacity: 0.3;
  transform: scale(0.9);
}

.lyric-line.active {
  color: var(--primary-color);
  font-size: 16px;
  font-weight: 600;
  text-shadow: 0 0 12px rgba(102, 126, 234, 0.4);
  transform: scale(1);
  opacity: 1;
}

.lyric-text {
  display: block;
}

.lyric-translation {
  display: block;
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
  font-weight: 400;
}

.lyric-line.active .lyric-translation {
  color: rgba(102, 126, 234, 0.6);
}

.lyric-empty, .lyric-loading {
  text-align: center;
  color: var(--text-secondary);
  padding: 20px 0;
}

.similar-section {
  background: var(--bg-secondary);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.similar-loading {
  text-align: center;
  color: var(--text-tertiary);
  padding: 40px 0;
}

.similar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.similar-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  transition: all 0.2s;
}

.similar-card:hover {
  border-color: var(--border-glow);
  transform: translateY(-2px);
  box-shadow: var(--card-shadow);
}

.similar-cover {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
}

.similar-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255,255,255,0.5);
}

.similar-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.4);
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
}

.similar-card:hover .similar-play {
  opacity: 1;
}

.similar-add {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  background: rgba(0,0,0,0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s;
  color: #fff;
  cursor: pointer;
}

.similar-add:hover {
  background: rgba(102,126,234,0.8);
  transform: scale(1.1);
}

.similar-card:hover .similar-add {
  opacity: 1;
}

.similar-info {
  padding: 10px;
}

.similar-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.similar-singer {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.comment-section {
  background: var(--bg-secondary);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.comment-input {
  margin-bottom: 16px;
}

.comment-input-actions {
  margin-top: 10px;
  text-align: right;
}

.comment-sort {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
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

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-main {
  display: flex;
  gap: 12px;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-nickname {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.comment-timestamp {
  font-size: 12px;
  color: var(--primary-color);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 3px;
  background: rgba(102, 126, 234, 0.1);
  padding: 1px 6px;
  border-radius: 4px;
  transition: background 0.2s;
}

.comment-timestamp:hover {
  background: rgba(102, 126, 234, 0.2);
}

.comment-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.comment-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-footer, .reply-footer {
  display: flex;
  gap: 16px;
}

.comment-action {
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}

.comment-action:hover {
  color: var(--primary-color);
}

.comment-action.liked {
  color: var(--accent-pink);
}

.comment-action.delete:hover {
  color: var(--accent-pink);
}

.reply-to {
  font-size: 12px;
  color: var(--text-secondary);
}

.reply-to-name {
  color: var(--primary-color);
}

.comment-replies {
  margin-top: 12px;
  margin-left: 48px;
  background: var(--bg-elevated);
  border-radius: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
}

.reply-item + .reply-item {
  border-top: 1px solid var(--border-color);
}

.reply-body {
  flex: 1;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.reply-nickname {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.reply-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.reply-content {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 6px;
}

.reply-input {
  margin-top: 12px;
  margin-left: 48px;
}

.reply-input-actions {
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

.disabled-like {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
