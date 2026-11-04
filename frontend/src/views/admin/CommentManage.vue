<template>
  <div class="comment-manage">
    <div class="page-header">
      <h2>评论管理</h2>
      <div class="header-actions">
        <div class="search-box">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
          <input v-model="keyword" placeholder="搜索评论内容" @keyup.enter="loadComments" />
        </div>
        <select v-model="statusFilter" class="filter-select" @change="loadComments">
          <option :value="null">全部状态</option>
          <option :value="1">正常</option>
          <option :value="0">已删除</option>
        </select>
        <select v-model="typeFilter" class="filter-select" @change="loadComments">
          <option value="">全部类型</option>
          <option value="song">歌曲评论</option>
          <option value="playlist">歌单评论</option>
        </select>
      </div>
    </div>

    <div class="data-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户</th>
            <th>评论内容</th>
            <th>类型</th>
            <th>点赞</th>
            <th>状态</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="comment in comments" :key="comment.id">
            <td class="td-id">{{ comment.id }}</td>
            <td>
              <div class="user-cell">
                <div class="user-avatar" v-if="comment.userAvatar">
                  <img :src="comment.userAvatar" alt="" />
                </div>
                <div class="user-avatar placeholder" v-else>
                  <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                </div>
                <span class="user-name">{{ comment.nickname || comment.username || '未知' }}</span>
              </div>
            </td>
            <td>
              <div class="comment-content" :class="{ deleted: comment.status === 0 }">
                {{ comment.content }}
              </div>
            </td>
            <td>
              <span class="type-tag" :class="comment.songId ? 'song' : comment.playlistId ? 'playlist' : 'other'">
                {{ comment.songId ? '歌曲' : comment.playlistId ? '歌单' : '其他' }}
              </span>
            </td>
            <td>
              <span class="like-count">
                <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
                {{ comment.likeCount || 0 }}
              </span>
            </td>
            <td>
              <span class="status-badge" :class="comment.status === 1 ? 'active' : 'disabled'">
                {{ comment.status === 1 ? '正常' : '已删除' }}
              </span>
            </td>
            <td class="td-time">{{ formatTime(comment.createTime) }}</td>
            <td>
              <div class="action-btns">
                <button class="btn btn-sm" :class="comment.status === 1 ? 'btn-danger' : 'btn-success'" @click="toggleStatus(comment)">
                  {{ comment.status === 1 ? '删除' : '恢复' }}
                </button>
                <button class="btn btn-sm btn-danger-outline" @click="deleteComment(comment)">永久删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span class="total-text">共 {{ pagination.total }} 条</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="pagination.page <= 0" @click="pagination.page--; loadComments()">上一页</button>
          <span class="page-info">{{ pagination.page + 1 }} / {{ Math.ceil(pagination.total / pagination.size) || 1 }}</span>
          <button class="page-btn" :disabled="(pagination.page + 1) * pagination.size >= pagination.total" @click="pagination.page++; loadComments()">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const comments = ref([])
const keyword = ref('')
const statusFilter = ref(null)
const typeFilter = ref('')
const pagination = reactive({ page: 0, size: 20, total: 0 })

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

const loadComments = async () => {
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    const res = await request.get('/admin/comments', { params })
    if (res.code === 200) {
      let list = res.data.list
      if (typeFilter.value === 'song') {
        list = list.filter(c => c.songId)
      } else if (typeFilter.value === 'playlist') {
        list = list.filter(c => c.playlistId)
      }
      if (keyword.value) {
        list = list.filter(c => c.content && c.content.includes(keyword.value))
      }
      comments.value = list
      pagination.total = typeFilter.value || keyword.value ? list.length : res.data.total
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const toggleStatus = async (row) => {
  try {
    const action = row.status === 1 ? '删除' : '恢复'
    await ElMessageBox.confirm(`确定${action}此评论吗？`, '提示', { type: 'warning' })
    await request.put(`/admin/comments/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success('操作成功')
    loadComments()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const deleteComment = async (row) => {
  try {
    await ElMessageBox.confirm('确定永久删除此评论吗？此操作不可恢复！', '警告', { type: 'error' })
    await request.delete(`/admin/comments/${row.id}`)
    ElMessage.success('删除成功')
    loadComments()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => loadComments())
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 8px 12px; color: rgba(255,255,255,0.4); }
.search-box input { background: none; border: none; outline: none; color: #fff; font-size: 13px; width: 160px; }
.search-box input::placeholder { color: rgba(255,255,255,0.3); }
.filter-select { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 8px 12px; color: #fff; font-size: 13px; outline: none; }
.filter-select option { background: #1a1a2e; }
.data-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { padding: 14px 16px; text-align: left; font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.4); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.data-table td { padding: 12px 16px; font-size: 13px; color: rgba(255,255,255,0.7); border-bottom: 1px solid rgba(255,255,255,0.04); }
.data-table tr:hover td { background: rgba(255,255,255,0.02); }
.td-id { color: rgba(255,255,255,0.3); font-size: 12px; }
.td-time { color: rgba(255,255,255,0.4); font-size: 12px; }
.user-cell { display: flex; align-items: center; gap: 8px; }
.user-avatar { width: 28px; height: 28px; border-radius: 50%; overflow: hidden; flex-shrink: 0; }
.user-avatar img { width: 100%; height: 100%; object-fit: cover; }
.user-avatar.placeholder { background: rgba(102,126,234,0.2); display: flex; align-items: center; justify-content: center; color: #667eea; }
.user-name { color: rgba(255,255,255,0.7); font-size: 13px; }
.comment-content { max-width: 300px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: rgba(255,255,255,0.7); }
.comment-content.deleted { color: rgba(255,255,255,0.3); text-decoration: line-through; }
.type-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; }
.type-tag.song { background: rgba(102,126,234,0.15); color: #667eea; }
.type-tag.playlist { background: rgba(79,172,254,0.15); color: #4facfe; }
.type-tag.other { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.4); }
.like-count { display: inline-flex; align-items: center; gap: 4px; color: rgba(255,255,255,0.4); font-size: 12px; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; }
.status-badge.active { background: rgba(67,233,123,0.15); color: #43e97b; }
.status-badge.disabled { background: rgba(245,87,108,0.15); color: #f5576c; }
.action-btns { display: flex; gap: 6px; }
.btn { padding: 6px 12px; border: none; border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-sm { padding: 4px 10px; }
.btn-danger { background: rgba(245,87,108,0.15); color: #f5576c; }
.btn-danger:hover { background: rgba(245,87,108,0.25); }
.btn-danger-outline { background: transparent; color: #f5576c; border: 1px solid rgba(245,87,108,0.3); }
.btn-danger-outline:hover { background: rgba(245,87,108,0.1); }
.btn-success { background: rgba(67,233,123,0.15); color: #43e97b; }
.btn-success:hover { background: rgba(67,233,123,0.25); }
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 16px; }
.total-text { font-size: 13px; color: rgba(255,255,255,0.4); }
.page-btns { display: flex; align-items: center; gap: 12px; }
.page-btn { padding: 6px 14px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; color: rgba(255,255,255,0.6); font-size: 13px; cursor: pointer; }
.page-btn:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 13px; color: rgba(255,255,255,0.5); }
</style>
