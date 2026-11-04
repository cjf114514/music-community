<template>
  <div class="community-page">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><ChatLineSquare /></el-icon> 音乐社区
      </h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索帖子..."
          :prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="loadPosts"
        />
        <el-button v-if="userStore.isLoggedIn()" type="primary" @click="showCreateDialog = true" class="create-btn">
          <el-icon><Plus /></el-icon> 发布帖子
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-wrapper">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else>
      <div v-if="posts.length" class="post-list">
        <div v-for="post in posts" :key="post.id" class="post-card">
          <div class="post-header">
            <el-avatar :size="40" :src="post.userAvatar">
              {{ post.userNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="post-user-info">
              <div class="post-user-name" @click="goToUserProfile(post)">
                {{ post.userNickname }}
              </div>
              <div class="post-time">{{ formatTime(post.createTime) }}</div>
            </div>
            <el-dropdown trigger="click" @command="(cmd) => handlePostAction(cmd, post)">
              <div class="post-more-btn">
                <el-icon><MoreFilled /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="isPostOwner(post)" command="edit">
                    <el-icon><Edit /></el-icon> 编辑
                  </el-dropdown-item>
                  <el-dropdown-item v-if="isPostOwner(post)" command="delete">
                    <el-icon><Delete /></el-icon> 删除
                  </el-dropdown-item>
                  <el-dropdown-item command="share">
                    <el-icon><Share /></el-icon> 分享
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="post-content" @click="goToPostDetail(post.id)">
            <h3 class="post-title">{{ post.title }}</h3>
            <p class="post-text">{{ post.content }}</p>
            <div v-if="post.imageUrls" class="post-images">
              <img
                v-for="(url, idx) in parseImageUrls(post.imageUrls)"
                :key="idx"
                :src="url"
                class="post-image-thumb"
                @click.stop="previewImage(url, parseImageUrls(post.imageUrls))"
              />
            </div>
            <div v-else-if="post.coverUrl" class="post-images">
              <img :src="post.coverUrl" class="post-image-thumb" @click.stop="previewImage(post.coverUrl, [post.coverUrl])" />
            </div>
            <div v-if="post.relatedSong" class="related-music-card" @click.stop="playRelatedSong(post.relatedSong)">
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
            <div v-if="post.relatedPlaylist" class="related-music-card" @click.stop="$router.push(`/playlist/${post.relatedPlaylist.id}`)">
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
          </div>
          <div class="post-footer">
            <div v-if="userStore.isLoggedIn()" class="post-action-btn" @click="handleLike(post)" :class="{ liked: post.liked }">
              <el-icon><Star /></el-icon> {{ post.likeCount }}
            </div>
            <div v-else class="post-action-btn disabled-action">
              <el-icon><Star /></el-icon> {{ post.likeCount }}
            </div>
            <div class="post-action-btn" @click="goToPostDetail(post.id)">
              <el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}
            </div>
            <div v-if="userStore.isLoggedIn()" class="post-action-btn" @click="handleShare(post)">
              <el-icon><Share /></el-icon> 分享
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无帖子，快来发布第一篇吧" />
    </template>

    <el-dialog v-model="showCreateDialog" :title="editingPost ? '编辑帖子' : '发布帖子'" width="600px" @close="resetForm">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="createForm.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="createForm.content" type="textarea" :rows="6" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="multi-upload-area">
            <div v-for="(url, idx) in createForm.imageList" :key="idx" class="upload-preview-item">
              <img :src="url" class="preview-image" />
              <div class="remove-btn" @click="removeImage(idx)"><el-icon><Delete /></el-icon></div>
            </div>
            <div v-if="createForm.imageList.length < 9" class="upload-add-btn" @click="triggerUpload">
              <el-icon :size="28"><Plus /></el-icon>
            </div>
          </div>
          <input ref="fileInputRef" type="file" accept="image/*" multiple style="display:none" @change="handleFileSelect" />
        </el-form-item>
        <el-form-item label="关联歌曲">
          <div class="related-select-row">
            <el-input v-model="songSearchKeyword" placeholder="搜索歌曲..." @keyup.enter="searchSongs" clearable />
            <el-button @click="searchSongs" :loading="songSearching">搜索</el-button>
          </div>
          <div v-if="createForm.relatedSong" class="selected-related">
            <el-icon><Music /></el-icon> {{ createForm.relatedSong.name }} - {{ createForm.relatedSong.singer }}
            <el-icon class="clear-related" @click="createForm.relatedSong = null; createForm.relatedSongId = null"><Close /></el-icon>
          </div>
          <div v-if="songSearchResults.length" class="search-results-dropdown">
            <div v-for="s in songSearchResults" :key="s.id" class="search-result-item" @click="selectSong(s)">
              <el-icon><Music /></el-icon> {{ s.name }} - {{ s.singer }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="关联歌单">
          <div class="related-select-row">
            <el-input v-model="playlistSearchKeyword" placeholder="搜索歌单..." @keyup.enter="searchPlaylists" clearable />
            <el-button @click="searchPlaylists" :loading="playlistSearching">搜索</el-button>
          </div>
          <div v-if="createForm.relatedPlaylist" class="selected-related">
            <el-icon><Music /></el-icon> {{ createForm.relatedPlaylist.name }}
            <el-icon class="clear-related" @click="createForm.relatedPlaylist = null; createForm.relatedPlaylistId = null"><Close /></el-icon>
          </div>
          <div v-if="playlistSearchResults.length" class="search-results-dropdown">
            <div v-for="p in playlistSearchResults" :key="p.id" class="search-result-item" @click="selectPlaylist(p)">
              <el-icon><Music /></el-icon> {{ p.name }}
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleSubmit">
          {{ editingPost ? '保存' : '发布' }}
        </el-button>
      </template>
    </el-dialog>

    <el-image-viewer
      v-if="showViewer"
      :url-list="viewerList"
      :initial-index="viewerIndex"
      @close="showViewer = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMusicStore } from '@/stores/music'
import {
  getPosts, createPost, updatePost, deletePost, likePost, getLikeStatus,
  uploadPostImage, searchPosts
} from '@/api/community'
import { searchSongs as searchSongsApi } from '@/api/song'
import { searchPlaylists as searchPlaylistsApi } from '@/api/song'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const musicStore = useMusicStore()
const loading = ref(true)
const posts = ref([])
const showCreateDialog = ref(false)
const creating = ref(false)
const editingPost = ref(null)
const createForm = ref({
  title: '',
  content: '',
  imageList: [],
  relatedSongId: null,
  relatedSong: null,
  relatedPlaylistId: null,
  relatedPlaylist: null
})

const songSearchKeyword = ref('')
const songSearchResults = ref([])
const songSearching = ref(false)
const playlistSearchKeyword = ref('')
const playlistSearchResults = ref([])
const playlistSearching = ref(false)

const searchKeyword = ref('')

const showViewer = ref(false)
const viewerList = ref([])
const viewerIndex = ref(0)

const fileInputRef = ref(null)

onMounted(async () => {
  await loadPosts()
})

const loadPosts = async () => {
  loading.value = true
  try {
    const res = await getPosts(50)
    posts.value = res.data || []
    if (userStore.userInfo?.userId) {
      for (const post of posts.value) {
        try {
          const statusRes = await getLikeStatus(post.id)
          post.liked = statusRes.data.liked
        } catch (e) {
          post.liked = false
        }
      }
    }
  } catch (e) {
    ElMessage.error('获取帖子失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    await loadPosts()
    return
  }
  loading.value = true
  try {
    const res = await searchPosts(searchKeyword.value.trim())
    posts.value = res.data || []
  } catch (e) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
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

const isPostOwner = (post) => {
  return userStore.userInfo?.userId && post.userId === userStore.userInfo.userId
}

const handlePostAction = (cmd, post) => {
  if (cmd === 'edit') {
    editingPost.value = post
    createForm.value = {
      title: post.title,
      content: post.content,
      imageList: post.imageUrls ? parseImageUrls(post.imageUrls) : (post.coverUrl ? [post.coverUrl] : []),
      relatedSongId: post.relatedSongId,
      relatedSong: post.relatedSong || null,
      relatedPlaylistId: post.relatedPlaylistId,
      relatedPlaylist: post.relatedPlaylist || null
    }
    showCreateDialog.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定要删除这篇帖子吗？', '提示', { type: 'warning' }).then(async () => {
      try {
        await deletePost(post.id)
        ElMessage.success('删除成功')
        await loadPosts()
      } catch (e) {
        ElMessage.error('删除失败')
      }
    }).catch(() => {})
  } else if (cmd === 'share') {
    handleShare(post)
  }
}

const handleShare = (post) => {
  const url = `${window.location.origin}/community/post/${post.id}`
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('链接已复制到剪贴板')
    }).catch(() => {
      fallbackCopy(url)
    })
  } else {
    fallbackCopy(url)
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

const resetForm = () => {
  editingPost.value = null
  createForm.value = {
    title: '',
    content: '',
    imageList: [],
    relatedSongId: null,
    relatedSong: null,
    relatedPlaylistId: null,
    relatedPlaylist: null
  }
  songSearchResults.value = []
  playlistSearchResults.value = []
}

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = async (e) => {
  const files = Array.from(e.target.files)
  for (const file of files) {
    if (createForm.value.imageList.length >= 9) break
    if (!file.type.startsWith('image/')) continue
    if (file.size / 1024 / 1024 > 5) {
      ElMessage.error(`${file.name} 超过5MB`)
      continue
    }
    try {
      const res = await uploadPostImage(file)
      createForm.value.imageList.push(res.data)
    } catch (err) {
      ElMessage.error(`${file.name} 上传失败`)
    }
  }
  e.target.value = ''
}

const removeImage = (idx) => {
  createForm.value.imageList.splice(idx, 1)
}

const searchSongs = async () => {
  if (!songSearchKeyword.value.trim()) return
  songSearching.value = true
  try {
    const res = await searchSongsApi(songSearchKeyword.value.trim())
    songSearchResults.value = (res.data || []).slice(0, 10)
  } catch (e) {
    ElMessage.error('搜索歌曲失败')
  } finally {
    songSearching.value = false
  }
}

const selectSong = (song) => {
  createForm.value.relatedSongId = song.id
  createForm.value.relatedSong = { id: song.id, name: song.name, singer: song.singer, coverUrl: song.coverUrl, album: song.album }
  songSearchResults.value = []
  songSearchKeyword.value = ''
}

const searchPlaylists = async () => {
  if (!playlistSearchKeyword.value.trim()) return
  playlistSearching.value = true
  try {
    const res = await searchPlaylistsApi(playlistSearchKeyword.value.trim())
    playlistSearchResults.value = (res.data || []).slice(0, 10)
  } catch (e) {
    ElMessage.error('搜索歌单失败')
  } finally {
    playlistSearching.value = false
  }
}

const selectPlaylist = (pl) => {
  createForm.value.relatedPlaylistId = pl.id
  createForm.value.relatedPlaylist = { id: pl.id, name: pl.name, coverUrl: pl.coverUrl }
  playlistSearchResults.value = []
  playlistSearchKeyword.value = ''
}

const handleSubmit = async () => {
  if (!createForm.value.title.trim() || !createForm.value.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  creating.value = true
  try {
    const payload = {
      title: createForm.value.title,
      content: createForm.value.content,
      coverUrl: createForm.value.imageList[0] || '',
      imageUrls: createForm.value.imageList.length ? JSON.stringify(createForm.value.imageList) : '',
      relatedSongId: createForm.value.relatedSongId,
      relatedPlaylistId: createForm.value.relatedPlaylistId
    }
    if (editingPost.value) {
      await updatePost(editingPost.value.id, payload)
      ElMessage.success('编辑成功')
    } else {
      await createPost(payload)
      ElMessage.success('发布成功')
    }
    showCreateDialog.value = false
    resetForm()
    await loadPosts()
  } catch (e) {
    ElMessage.error(editingPost.value ? '编辑失败' : '发布失败')
  } finally {
    creating.value = false
  }
}

const handleLike = async (post) => {
  if (!userStore.userInfo?.userId) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await likePost(post.id)
    const liked = res.data.liked
    post.liked = liked
    post.likeCount += liked ? 1 : -1
    ElMessage.success(liked ? '点赞成功' : '已取消点赞')
  } catch (e) {
    ElMessage.error('操作失败')
  }
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

const goToPostDetail = (postId) => {
  router.push(`/community/post/${postId}`)
}

const goToUserProfile = (post) => {
  router.push({
    path: `/user/${post.userId}`,
    query: { nickname: post.userNickname, avatar: post.userAvatar }
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
.community-page {
  padding: 0 0 40px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.page-title .el-icon {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 220px;
}

.create-btn {
  background: var(--gradient-primary) !important;
  border: none !important;
  box-shadow: var(--neon-glow-sm) !important;
}

.create-btn:hover {
  box-shadow: var(--neon-glow) !important;
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

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: var(--bg-secondary);
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--card-shadow);
  transition: all 0.3s;
  border: 1px solid var(--border-color);
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--card-hover-shadow);
  border-color: var(--border-glow);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.post-user-info {
  flex: 1;
}

.post-user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
  transition: color 0.2s;
}

.post-user-name:hover {
  color: var(--primary-color);
}

.post-time {
  font-size: 12px;
  color: var(--text-tertiary);
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

.post-content {
  cursor: pointer;
}

.post-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.post-text {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 12px;
}

.post-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.post-image-thumb {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 10px;
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
  margin-top: 12px;
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

.post-footer {
  display: flex;
  gap: 16px;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
  margin-top: 12px;
}

.post-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.post-action-btn:hover {
  color: var(--primary-color);
}

.post-action-btn.liked {
  color: var(--accent-pink);
}

.multi-upload-area {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.upload-preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.upload-preview-item .preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-preview-item .remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.2s;
}

.upload-preview-item .remove-btn:hover {
  background: rgba(220,38,38,0.8);
}

.upload-add-btn {
  width: 100px;
  height: 100px;
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.upload-add-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.related-select-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.related-select-row .el-input {
  flex: 1;
}

.selected-related {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 6px 10px;
  background: var(--bg-elevated);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-primary);
}

.clear-related {
  margin-left: auto;
  cursor: pointer;
  color: var(--text-tertiary);
  transition: color 0.2s;
}

.clear-related:hover {
  color: var(--accent-pink);
}

.search-results-dropdown {
  margin-top: 8px;
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-elevated);
}

.search-result-item {
  padding: 8px 12px;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.2s;
}

.search-result-item:hover {
  background: var(--nav-hover-bg);
}

.disabled-action {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
