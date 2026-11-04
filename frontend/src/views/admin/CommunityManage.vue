<template>
  <div class="community-manage">
    <div class="page-header">
      <h2>社区管理</h2>
      <div class="header-actions">
        <div class="search-box">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
          <input v-model="keyword" placeholder="搜索帖子标题或内容" @keyup.enter="loadPosts" />
        </div>
      </div>
    </div>

    <div class="stats-row">
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
          <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
        </div>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ pagination.total }}</span>
          <span class="mini-stat-label">帖子总数</span>
        </div>
      </div>
    </div>

    <div class="posts-grid">
      <div class="post-card" v-for="post in posts" :key="post.id">
        <div class="post-header">
          <div class="post-author">
            <div class="author-avatar" v-if="post.userAvatar">
              <img :src="post.userAvatar" alt="" />
            </div>
            <div class="author-avatar placeholder" v-else>
              <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <div class="author-info">
              <span class="author-name">{{ post.nickname || post.username || '未知用户' }}</span>
              <span class="post-time">{{ formatTime(post.createTime) }}</span>
            </div>
          </div>
          <button class="btn btn-sm btn-danger-outline" @click="deletePost(post)">删除</button>
        </div>

        <div class="post-body">
          <h4 class="post-title">{{ post.title }}</h4>
          <p class="post-content">{{ post.content }}</p>
          <div class="post-cover" v-if="post.coverUrl">
            <img :src="post.coverUrl" alt="" />
          </div>
        </div>

        <div class="post-footer">
          <div class="post-stats">
            <span class="stat-item">
              <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
              {{ post.likeCount || 0 }}
            </span>
            <span class="stat-item">
              <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M21 6h-2v9H6v2c0 .55.45 1 1 1h11l4 4V7c0-.55-.45-1-1-1zm-4 6V3c0-.55-.45-1-1-1H3c-.55 0-1 .45-1 1v14l4-4h10c.55 0 1-.45 1-1z"/></svg>
              {{ post.commentCount || 0 }}
            </span>
          </div>
          <button class="btn btn-sm btn-ghost" @click="viewComments(post)">
            查看评论
          </button>
        </div>
      </div>

      <div class="empty-state" v-if="!posts.length">暂无帖子数据</div>
    </div>

    <div class="pagination" v-if="pagination.total > pagination.size">
      <span class="total-text">共 {{ pagination.total }} 条</span>
      <div class="page-btns">
        <button class="page-btn" :disabled="pagination.page <= 0" @click="pagination.page--; loadPosts()">上一页</button>
        <span class="page-info">{{ pagination.page + 1 }} / {{ Math.ceil(pagination.total / pagination.size) || 1 }}</span>
        <button class="page-btn" :disabled="(pagination.page + 1) * pagination.size >= pagination.total" @click="pagination.page++; loadPosts()">下一页</button>
      </div>
    </div>

    <el-dialog v-model="commentDialogVisible" :title="`帖子评论 - ${currentPost?.title || ''}`" width="640px" :append-to-body="true" class="admin-community-dialog">
      <div class="comment-list" v-if="postComments.length">
        <!-- eslint-disable-next-line vue/no-v-for-template-key -->
        <template v-for="comment in postComments" :key="comment.id">
          <div class="comment-item">
            <div class="comment-author">
              <div class="comment-avatar" v-if="comment.userAvatar">
                <img :src="comment.userAvatar" alt="" />
              </div>
              <div class="comment-avatar placeholder" v-else>
                <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              </div>
              <span class="comment-name">{{ comment.nickname || comment.username || '未知' }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-text">{{ comment.content }}</div>
            <div class="comment-actions">
              <span class="comment-likes">
                <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
                {{ comment.likeCount || 0 }}
              </span>
              <button class="btn btn-sm btn-danger-outline" @click="deleteComment(comment)">删除</button>
            </div>
          </div>
          <div class="reply-list" v-if="comment.replies && comment.replies.length">
            <div class="reply-item" v-for="reply in comment.replies" :key="reply.id">
              <div class="comment-author">
                <div class="comment-avatar" v-if="reply.userAvatar">
                  <img :src="reply.userAvatar" alt="" />
                </div>
                <div class="comment-avatar placeholder" v-else>
                  <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                </div>
                <span class="comment-name">{{ reply.nickname || reply.username || '未知' }}</span>
                <span class="reply-tag">回复</span>
                <span class="comment-name reply-target">@{{ comment.nickname || comment.username || '未知' }}</span>
                <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
              </div>
              <div class="comment-text">{{ reply.content }}</div>
              <div class="comment-actions">
                <span class="comment-likes">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
                  {{ reply.likeCount || 0 }}
                </span>
                <button class="btn btn-sm btn-danger-outline" @click="deleteComment(reply)">删除</button>
              </div>
            </div>
          </div>
        </template>
      </div>
      <div class="empty-state" v-else>暂无评论</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const posts = ref([])
const keyword = ref('')
const pagination = reactive({ page: 0, size: 20, total: 0 })

const commentDialogVisible = ref(false)
const currentPost = ref(null)
const postComments = ref([])

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

const loadPosts = async () => {
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (keyword.value) params.keyword = keyword.value
    const res = await request.get('/admin/community/posts', { params })
    if (res.code === 200) {
      posts.value = res.data.list
      pagination.total = res.data.total
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const deletePost = async (post) => {
  try {
    await ElMessageBox.confirm(`确定删除帖子「${post.title}」吗？相关评论也会被删除。`, '警告', { type: 'warning' })
    await request.delete(`/admin/community/posts/${post.id}`)
    ElMessage.success('删除成功')
    loadPosts()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

const viewComments = async (post) => {
  currentPost.value = post
  commentDialogVisible.value = true
  try {
    const res = await request.get(`/admin/community/posts/${post.id}/comments`, { params: { page: 0, size: 50 } })
    if (res.code === 200) {
      postComments.value = res.data.list
    }
  } catch (e) {
    ElMessage.error('加载评论失败')
  }
}

const deleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确定删除此评论吗？', '警告', { type: 'warning' })
    await request.delete(`/admin/community/comments/${comment.id}`)
    ElMessage.success('删除成功')
    if (currentPost.value) viewComments(currentPost.value)
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => loadPosts())
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 8px 12px; color: rgba(255,255,255,0.4); }
.search-box input { background: none; border: none; outline: none; color: #fff; font-size: 13px; width: 200px; }
.search-box input::placeholder { color: rgba(255,255,255,0.3); }

.stats-row { display: flex; gap: 16px; margin-bottom: 20px; }
.mini-stat { display: flex; align-items: center; gap: 12px; background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 16px 20px; }
.mini-stat-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; }
.mini-stat-value { font-size: 22px; font-weight: 700; color: #fff; display: block; }
.mini-stat-label { font-size: 12px; color: rgba(255,255,255,0.4); }

.posts-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 16px; }
.post-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 14px; transition: all 0.3s; }
.post-card:hover { border-color: rgba(102,126,234,0.3); }

.post-header { display: flex; align-items: center; justify-content: space-between; }
.post-author { display: flex; align-items: center; gap: 10px; }
.author-avatar { width: 32px; height: 32px; border-radius: 50%; overflow: hidden; flex-shrink: 0; }
.author-avatar img { width: 100%; height: 100%; object-fit: cover; }
.author-avatar.placeholder { background: rgba(102,126,234,0.2); display: flex; align-items: center; justify-content: center; color: #667eea; }
.author-info { display: flex; flex-direction: column; gap: 2px; }
.author-name { font-size: 13px; color: rgba(255,255,255,0.8); font-weight: 500; }
.post-time { font-size: 11px; color: rgba(255,255,255,0.3); }

.post-body { display: flex; flex-direction: column; gap: 8px; }
.post-title { font-size: 15px; font-weight: 600; color: #fff; margin: 0; line-height: 1.4; }
.post-content { font-size: 13px; color: rgba(255,255,255,0.5); line-height: 1.6; margin: 0; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.post-cover { border-radius: 8px; overflow: hidden; max-height: 180px; }
.post-cover img { width: 100%; height: 100%; object-fit: cover; }

.post-footer { display: flex; align-items: center; justify-content: space-between; padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.04); }
.post-stats { display: flex; gap: 16px; }
.stat-item { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: rgba(255,255,255,0.4); }

.comment-list { display: flex; flex-direction: column; gap: 12px; max-height: 400px; overflow-y: auto; }
.comment-list::-webkit-scrollbar { width: 4px; }
.comment-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 2px; }
.comment-item { padding: 12px; background: rgba(255,255,255,0.04); border-radius: 8px; }
.comment-author { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.comment-avatar { width: 24px; height: 24px; border-radius: 50%; overflow: hidden; }
.comment-avatar img { width: 100%; height: 100%; object-fit: cover; }
.comment-avatar.placeholder { background: rgba(102,126,234,0.2); display: flex; align-items: center; justify-content: center; color: #667eea; }
.comment-name { font-size: 12px; color: var(--text-primary, rgba(255,255,255,0.7)); font-weight: 500; }
.comment-time { font-size: 11px; color: var(--text-tertiary, rgba(255,255,255,0.3)); }
.comment-text { font-size: 13px; color: var(--text-secondary, rgba(255,255,255,0.6)); line-height: 1.5; margin-bottom: 8px; }
.comment-actions { display: flex; align-items: center; justify-content: space-between; }
.comment-likes { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; color: var(--text-tertiary, rgba(255,255,255,0.3)); }

.reply-list { margin-left: 20px; padding-left: 12px; border-left: 2px solid rgba(102,126,234,0.2); display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.reply-item { padding: 10px; background: rgba(102,126,234,0.04); border-radius: 6px; }
.reply-tag { font-size: 11px; color: rgba(102,126,234,0.6); margin: 0 2px; }
.reply-target { color: rgba(102,126,234,0.8); font-size: 12px; }

.btn { padding: 6px 12px; border: none; border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-sm { padding: 4px 10px; }
.btn-primary { background: rgba(102,126,234,0.2); color: #667eea; }
.btn-primary:hover { background: rgba(102,126,234,0.3); }
.btn-danger-outline { background: transparent; color: #f5576c; border: 1px solid rgba(245,87,108,0.3); }
.btn-danger-outline:hover { background: rgba(245,87,108,0.1); }
.btn-ghost { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.5); }
.btn-ghost:hover { background: rgba(255,255,255,0.1); }

.empty-state { text-align: center; padding: 60px; color: rgba(255,255,255,0.2); font-size: 14px; grid-column: 1 / -1; }
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 16px 0; }
.total-text { font-size: 13px; color: rgba(255,255,255,0.4); }
.page-btns { display: flex; align-items: center; gap: 12px; }
.page-btn { padding: 6px 14px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; color: rgba(255,255,255,0.6); font-size: 13px; cursor: pointer; }
.page-btn:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 13px; color: rgba(255,255,255,0.5); }
</style>

<style>
.admin-community-dialog .el-dialog {
  background: #1a1a2e !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
}
.admin-community-dialog .el-dialog__header {
  border-bottom: 1px solid rgba(255,255,255,0.06) !important;
}
.admin-community-dialog .el-dialog__title {
  color: #fff !important;
}
.admin-community-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(255,255,255,0.5) !important;
}
.admin-community-dialog .el-dialog__body {
  background: #1a1a2e !important;
  color: rgba(255,255,255,0.7) !important;
}
</style>
