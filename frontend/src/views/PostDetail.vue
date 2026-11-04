<template>
  <div class="post-detail-page">
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="post">
      <el-card class="post-card">
        <div class="post-header">
          <el-avatar :size="50" :src="post.userAvatar">
            {{ post.userNickname?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="post-user-info">
            <div class="post-user-name" @click="goToUserProfile(post)">
              {{ post.userNickname }}
            </div>
            <div class="post-time">{{ formatTime(post.createTime) }}</div>
          </div>
          <el-dropdown trigger="click" @command="(cmd) => handlePostAction(cmd)">
            <div class="post-more-btn">
              <el-icon><MoreFilled /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="isPostOwner" command="edit">
                  <el-icon><Edit /></el-icon> 编辑
                </el-dropdown-item>
                <el-dropdown-item v-if="isPostOwner" command="delete">
                  <el-icon><Delete /></el-icon> 删除
                </el-dropdown-item>
                <el-dropdown-item command="share">
                  <el-icon><Share /></el-icon> 分享
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-content-text">{{ post.content }}</div>
        <div v-if="post.imageUrls" class="post-images">
          <img
            v-for="(url, idx) in parseImageUrls(post.imageUrls)"
            :key="idx"
            :src="url"
            class="post-image-thumb"
            @click="previewImage(url, parseImageUrls(post.imageUrls))"
          />
        </div>
        <div v-else-if="post.coverUrl" class="post-images">
          <img :src="post.coverUrl" class="post-image-thumb" @click="previewImage(post.coverUrl, [post.coverUrl])" />
        </div>
        <div v-if="post.relatedSong" class="related-music-card" @click="playRelatedSong(post.relatedSong)">
          <div class="rm-cover">
            <img v-if="post.relatedSong.coverUrl" :src="post.relatedSong.coverUrl" />
            <div v-else class="rm-placeholder"><el-icon :size="20"><Music /></el-icon></div>
            <div class="rm-play-icon always-show"><el-icon :size="20" color="#fff"><VideoPlay /></el-icon></div>
          </div>
          <div class="rm-info">
            <div class="rm-name">{{ post.relatedSong.name }}</div>
            <div class="rm-singer">{{ post.relatedSong.singer }}</div>
          </div>
          <div class="rm-play-btn">
            <el-icon><VideoPlay /></el-icon> 播放
          </div>
        </div>
        <div v-if="post.relatedPlaylist" class="related-music-card" @click="$router.push(`/playlist/${post.relatedPlaylist.id}`)">
          <div class="rm-cover">
            <img v-if="post.relatedPlaylist.coverUrl" :src="post.relatedPlaylist.coverUrl" />
            <div v-else class="rm-placeholder"><el-icon :size="20"><Music /></el-icon></div>
          </div>
          <div class="rm-info">
            <div class="rm-name">{{ post.relatedPlaylist.name }}</div>
            <div class="rm-singer">歌单</div>
          </div>
          <el-icon class="rm-link"><ArrowRight /></el-icon>
        </div>
      </el-card>

      <div class="comments-section">
        <div class="comments-header">
          <h3>评论 ({{ post.commentCount }})</h3>
          <div class="sort-tabs">
            <span :class="['sort-tab', { active: sortBy === 'time' }]" @click="switchSort('time')">
              <el-icon><Clock /></el-icon> 最新
            </span>
            <span :class="['sort-tab', { active: sortBy === 'hot' }]" @click="switchSort('hot')">
              <el-icon><StarFilled /></el-icon> 最热
            </span>
          </div>
        </div>

        <div v-if="userStore.isLoggedIn()" class="comment-input">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
          />
          <el-button type="primary" @click="handleComment(null)" :loading="commenting">
            发表评论
          </el-button>
        </div>
        <div v-else class="guest-login-hint">
          <span>登录后即可发表评论</span>
          <el-button type="primary" size="small" @click="$router.push('/login')">去登录</el-button>
        </div>

        <div v-if="comments.length" class="comments-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <el-avatar :size="32" :src="comment.userAvatar">
              {{ comment.userNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="comment-body">
              <div class="comment-user" @click="goToUserProfile(comment)">
                {{ comment.userNickname }}
              </div>
              <div class="comment-text">{{ comment.content }}</div>
              <div class="comment-meta">
                <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                <span v-if="userStore.isLoggedIn()" class="comment-like" @click="handleLikeComment(comment)" :class="{ liked: comment.liked }">
                  <el-icon><component :is="comment.liked ? 'StarFilled' : 'Star'" /></el-icon> {{ comment.likeCount || 0 }}
                </span>
                <span v-else class="comment-like disabled-action">
                  <el-icon><Star /></el-icon> {{ comment.likeCount || 0 }}
                </span>
                <span v-if="userStore.isLoggedIn()" class="comment-reply-btn" @click="toggleReplyInput(comment.id)">
                  <el-icon><ChatDotRound /></el-icon> 回复
                </span>
                <span class="comment-reply-btn" @click="toggleReplies(comment)">
                  <el-icon><ArrowDown /></el-icon>
                  {{ comment.replyCount || 0 }}条回复
                </span>
                <el-dropdown v-if="isCommentOwner(comment)" trigger="click" @command="(cmd) => handleCommentAction(cmd, comment)">
                  <span class="comment-more-btn"><el-icon><MoreFilled /></el-icon></span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="delete" style="color: #f5576c;">
                        <el-icon><Delete /></el-icon> 删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <div v-if="replyingTo === comment.id" class="reply-input">
                <el-input
                  v-model="replyContent"
                  size="small"
                  :placeholder="`回复 ${comment.userNickname}...`"
                  @keyup.enter="handleComment(comment.id)"
                />
                <el-button size="small" type="primary" @click="handleComment(comment.id)" :loading="commenting">发送</el-button>
                <el-button size="small" @click="replyingTo = null">取消</el-button>
              </div>

              <div v-if="expandedReplies[comment.id]" class="replies-list">
                <div v-if="loadingReplies[comment.id]" class="replies-loading">
                  <el-icon class="loading-icon"><Loading /></el-icon> 加载中...
                </div>
                <div v-else-if="replies[comment.id]?.length" >
                  <div v-for="reply in replies[comment.id]" :key="reply.id" class="reply-item">
                    <el-avatar :size="24" :src="reply.userAvatar">
                      {{ reply.userNickname?.charAt(0) || 'U' }}
                    </el-avatar>
                    <div class="reply-body">
                      <span class="reply-user" @click="goToUserProfile(reply)">{{ reply.userNickname }}</span>
                      <span class="reply-text">{{ reply.content }}</span>
                      <div class="reply-meta">
                        <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                        <span class="comment-like" @click="handleLikeComment(reply, true, comment.id)" :class="{ liked: reply.liked }">
                          <el-icon><component :is="reply.liked ? 'StarFilled' : 'Star'" /></el-icon> {{ reply.likeCount || 0 }}
                        </span>
                        <span class="comment-reply-btn" @click="toggleReplyInput(comment.id, reply.userNickname)">
                          <el-icon><ChatDotRound /></el-icon> 回复
                        </span>
                        <el-dropdown v-if="isCommentOwner(reply)" trigger="click" @command="(cmd) => handleCommentAction(cmd, reply, comment.id)">
                          <span class="comment-more-btn" style="font-size: 12px;"><el-icon :size="12"><MoreFilled /></el-icon></span>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-dropdown-item command="delete" style="color: #f5576c;">
                                <el-icon><Delete /></el-icon> 删除
                              </el-dropdown-item>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="no-replies">暂无回复</div>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无评论" />
      </div>
    </template>

    <el-image-viewer
      v-if="showViewer"
      :url-list="viewerList"
      :initial-index="viewerIndex"
      @close="showViewer = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMusicStore } from '@/stores/music'
import {
  getPost, createComment, getPostComments, likeComment,
  getCommentReplies, deletePost, updatePost, deleteComment
} from '@/api/community'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const musicStore = useMusicStore()
const loading = ref(true)
const post = ref(null)
const comments = ref([])
const newComment = ref('')
const commenting = ref(false)
const sortBy = ref('time')
const replyingTo = ref(null)
const replyContent = ref('')
const expandedReplies = reactive({})
const replies = reactive({})
const loadingReplies = reactive({})
const showViewer = ref(false)
const viewerList = ref([])
const viewerIndex = ref(0)

const postId = Number(route.params.id)

onMounted(async () => {
  await loadPost()
  await loadComments()
})

const isPostOwner = ref(false)

const loadPost = async () => {
  loading.value = true
  try {
    const res = await getPost(postId)
    post.value = res.data
    isPostOwner.value = userStore.userInfo?.userId && res.data.userId === userStore.userInfo.userId
  } catch (e) {
    ElMessage.error('获取帖子失败')
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  try {
    const res = await getPostComments(postId, sortBy.value)
    comments.value = res.data || []
    if (post.value) {
      let total = comments.value.length
      comments.value.forEach(c => { total += c.replyCount || 0 })
      post.value.commentCount = total
    }
  } catch (e) {}
}

const switchSort = (type) => {
  if (sortBy.value === type) return
  sortBy.value = type
  loadComments()
}

const parseImageUrls = (urls) => {
  if (!urls) return []
  try {
    return JSON.parse(urls)
  } catch {
    return urls.split(',').filter(Boolean)
  }
}

const previewImage = (url, list) => {
  viewerList.value = list
  viewerIndex.value = list.indexOf(url)
  showViewer.value = true
}

const playRelatedSong = (song) => {
  musicStore.setCurrentSong({
    id: song.id,
    name: song.name,
    singer: song.singer,
    coverUrl: song.coverUrl,
    album: song.album,
    url: song.url,
    duration: song.duration
  })
}

const handlePostAction = (cmd) => {
  if (cmd === 'delete') {
    ElMessageBox.confirm('确定要删除这篇帖子吗？', '提示', { type: 'warning' }).then(async () => {
      try {
        await deletePost(postId)
        ElMessage.success('删除成功')
        router.push('/community')
      } catch (e) {
        ElMessage.error('删除失败')
      }
    }).catch(() => {})
  } else if (cmd === 'share') {
    const url = `${window.location.origin}/community/post/${postId}`
    if (navigator.clipboard) {
      navigator.clipboard.writeText(url).then(() => {
        ElMessage.success('链接已复制到剪贴板')
      }).catch(() => {
        fallbackCopy(url)
      })
    } else {
      fallbackCopy(url)
    }
  } else if (cmd === 'edit') {
    router.push(`/community?edit=${postId}`)
  }
}

const fallbackCopy = (text) => {
  const ta = document.createElement('textarea')
  ta.value = text
  document.body.appendChild(ta)
  ta.select()
  document.execCommand('copy')
  document.body.removeChild(ta)
  ElMessage.success('链接已复制到剪贴板')
}

const toggleReplyInput = (commentId, nickname) => {
  if (replyingTo.value === commentId) {
    replyingTo.value = null
    replyContent.value = ''
  } else {
    replyingTo.value = commentId
    replyContent.value = nickname ? `@${nickname} ` : ''
  }
}

const toggleReplies = async (comment) => {
  if (expandedReplies[comment.id]) {
    expandedReplies[comment.id] = false
    return
  }
  expandedReplies[comment.id] = true
  loadingReplies[comment.id] = true
  try {
    const res = await getCommentReplies(postId, comment.id)
    replies[comment.id] = res.data || []
  } catch (e) {
    replies[comment.id] = []
  } finally {
    loadingReplies[comment.id] = false
  }
}

const handleComment = async (parentId) => {
  const content = parentId ? replyContent.value.trim() : newComment.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commenting.value = true
  try {
    await createComment(postId, { content, parentId })
    ElMessage.success('评论成功')
    if (parentId) {
      replyContent.value = ''
      replyingTo.value = null
      const parentComment = comments.value.find(c => c.id === parentId)
      if (parentComment) {
        parentComment.replyCount = (parentComment.replyCount || 0) + 1
      }
      if (expandedReplies[parentId]) {
        const res = await getCommentReplies(postId, parentId)
        replies[parentId] = res.data || []
      }
    } else {
      newComment.value = ''
    }
    await loadPost()
    await loadComments()
  } catch (e) {
    ElMessage.error('评论失败')
  } finally {
    commenting.value = false
  }
}

const handleLikeComment = async (comment, isReply = false, parentCommentId = null) => {
  try {
    const res = await likeComment(comment.id)
    const isNowLiked = res.data.liked
    comment.liked = isNowLiked
    comment.likeCount = isNowLiked ? (comment.likeCount || 0) + 1 : Math.max(0, (comment.likeCount || 0) - 1)
  } catch (e) {
    ElMessage.error('点赞失败')
  }
}

const isCommentOwner = (comment) => {
  return userStore.isLoggedIn() && userStore.userInfo?.userId && comment.userId === userStore.userInfo.userId
}

const handleCommentAction = (cmd, comment, parentCommentId = null) => {
  if (cmd === 'delete') {
    ElMessageBox.confirm('确定要删除这条评论吗？', '提示', { type: 'warning' }).then(async () => {
      try {
        await deleteComment(comment.id)
        ElMessage.success('删除成功')
        if (parentCommentId) {
          const parentComment = comments.value.find(c => c.id === parentCommentId)
          if (parentComment) {
            parentComment.replyCount = Math.max(0, (parentComment.replyCount || 0) - 1)
          }
          if (expandedReplies[parentCommentId]) {
            const res = await getCommentReplies(postId, parentCommentId)
            replies[parentCommentId] = res.data || []
          }
        }
        await loadPost()
        await loadComments()
      } catch (e) {
        ElMessage.error('删除失败')
      }
    }).catch(() => {})
  }
}

const goToUserProfile = (item) => {
  router.push({
    path: `/user/${item.userId}`,
    query: { nickname: item.userNickname, avatar: item.userAvatar }
  })
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString()
}
</script>

<style scoped>
.post-detail-page {
  padding: 20px 0;
  max-width: 800px;
  margin: 0 auto;
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

.post-card {
  margin-bottom: 24px;
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.post-user-info {
  flex: 1;
}

.post-user-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
}

.post-user-name:hover {
  color: var(--primary-color);
}

.post-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.post-more-btn {
  padding: 6px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-tertiary);
  transition: all 0.2s;
}

.post-more-btn:hover {
  background: var(--nav-hover-bg);
  color: var(--text-primary);
}

.post-title {
  font-size: 24px;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.post-content-text {
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.8;
  margin-bottom: 16px;
  white-space: pre-wrap;
}

.post-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.post-image-thumb {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: all 0.2s;
}

.post-image-thumb:hover {
  opacity: 0.9;
  border-color: var(--border-glow);
}

.related-music-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: var(--bg-elevated);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.related-music-card:hover {
  border-color: var(--border-glow);
  background: var(--nav-hover-bg);
}

.rm-cover {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.rm-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rm-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  color: #fff;
}

.rm-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.rm-play-icon.always-show {
  opacity: 1;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.related-music-card:hover .rm-play-icon:not(.always-show) {
  opacity: 1;
}

.rm-info {
  flex: 1;
  overflow: hidden;
}

.rm-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rm-singer {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rm-play-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  background: var(--gradient-primary);
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.related-music-card:hover .rm-play-btn {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transform: scale(1.05);
}

.comments-section h3 {
  color: var(--text-primary);
  margin: 0;
}

.comments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.sort-tabs {
  display: flex;
  gap: 16px;
}

.sort-tab {
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.sort-tab:hover {
  color: var(--text-primary);
  background: var(--bg-primary);
}

.sort-tab.active {
  color: var(--primary-color);
  font-weight: 500;
}

.comment-input {
  margin-bottom: 20px;
}

.comment-input .el-button {
  margin-top: 8px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-primary);
}

.comment-body {
  flex: 1;
}

.comment-user {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
}

.comment-user:hover {
  color: var(--primary-color);
}

.comment-text {
  color: var(--text-primary);
  font-size: 14px;
  margin: 4px 0;
}

.comment-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.comment-like {
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 2px;
  transition: color 0.2s;
}

.comment-like:hover {
  color: var(--primary-color);
}

.comment-like.liked {
  color: #e74c3c;
}

.comment-reply-btn {
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 2px;
  transition: color 0.2s;
}

.comment-reply-btn:hover {
  color: var(--primary-color);
}

.comment-more-btn {
  cursor: pointer;
  color: var(--text-tertiary);
  padding: 2px;
  border-radius: 4px;
  transition: all 0.2s;
}

.comment-more-btn:hover {
  background: var(--nav-hover-bg);
  color: var(--text-primary);
}

.reply-input {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 8px;
}

.reply-input .el-input {
  flex: 1;
}

.replies-list {
  margin-top: 8px;
  padding-left: 12px;
  border-left: 2px solid var(--border-color);
}

.replies-loading {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.reply-item {
  display: flex;
  gap: 8px;
  padding: 8px 0;
}

.reply-body {
  flex: 1;
}

.reply-user {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
  margin-right: 4px;
}

.reply-user:hover {
  color: var(--primary-color);
}

.reply-text {
  font-size: 13px;
  color: var(--text-primary);
}

.reply-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 2px;
}

.no-replies {
  padding: 8px 0;
  font-size: 13px;
  color: var(--text-tertiary);
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
