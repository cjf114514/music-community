<template>
  <div class="playlist-manage">
    <div class="page-header">
      <h2>歌单管理</h2>
      <div class="header-actions">
        <div class="search-box">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
          <input v-model="keyword" placeholder="搜索歌单名" @keyup.enter="loadPlaylists" />
        </div>
        <select v-model="statusFilter" class="filter-select" @change="loadPlaylists">
          <option :value="null">全部状态</option>
          <option :value="1">正常</option>
          <option :value="0">下架</option>
        </select>
      </div>
    </div>

    <div class="data-card">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:50px"></th>
            <th>歌单信息</th>
            <th>创建者</th>
            <th>歌曲数</th>
            <th>播放量</th>
            <th>公开</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="pl in playlists" :key="pl.id">
            <td>
              <div class="pl-cover" v-if="pl.coverUrl"><img :src="pl.coverUrl" alt="" /></div>
              <div class="pl-cover placeholder" v-else>
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z"/></svg>
              </div>
            </td>
            <td>
              <div class="pl-name-cell">
                <span class="pl-name">{{ pl.name }}</span>
                <span class="pl-desc" v-if="pl.description">{{ pl.description }}</span>
              </div>
            </td>
            <td>{{ pl.nickname || pl.username || '-' }}</td>
            <td>{{ pl.songCount || 0 }}</td>
            <td>{{ formatCount(pl.playCount) }}</td>
            <td>
              <span class="status-badge" :class="pl.isPublic === 1 ? 'active' : 'private'">
                {{ pl.isPublic === 1 ? '公开' : '私密' }}
              </span>
            </td>
            <td>
              <span class="status-badge" :class="pl.status === 1 ? 'active' : 'disabled'">
                {{ pl.status === 1 ? '正常' : '下架' }}
              </span>
            </td>
            <td class="td-time">{{ formatTime(pl.createTime) }}</td>
            <td>
              <div class="action-btns">
                <button class="btn btn-sm" :class="pl.status === 1 ? 'btn-danger' : 'btn-success'" @click="toggleStatus(pl)">
                  {{ pl.status === 1 ? '下架' : '上架' }}
                </button>
                <button class="btn btn-sm btn-danger-outline" @click="deletePlaylist(pl)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span class="total-text">共 {{ pagination.total }} 条</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="pagination.page <= 0" @click="pagination.page--; loadPlaylists()">上一页</button>
          <span class="page-info">{{ pagination.page + 1 }} / {{ Math.ceil(pagination.total / pagination.size) || 1 }}</span>
          <button class="page-btn" :disabled="(pagination.page + 1) * pagination.size >= pagination.total" @click="pagination.page++; loadPlaylists()">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const playlists = ref([])
const keyword = ref('')
const statusFilter = ref(null)
const pagination = reactive({ page: 0, size: 20, total: 0 })

const formatCount = (n) => { if (!n) return '0'; if (n >= 10000) return (n / 10000).toFixed(1) + '万'; return n.toLocaleString() }
const formatTime = (t) => { if (!t) return '-'; return t.replace('T', ' ').slice(0, 16) }

const loadPlaylists = async () => {
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    const res = await request.get('/admin/playlists', { params })
    if (res.code === 200) { playlists.value = res.data.list; pagination.total = res.data.total }
  } catch (e) { ElMessage.error('加载失败') }
}

const toggleStatus = async (row) => {
  try {
    await request.put(`/admin/playlists/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success('操作成功'); loadPlaylists()
  } catch (e) { ElMessage.error('操作失败') }
}

const deletePlaylist = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除此歌单吗？', '警告', { type: 'warning' })
    await request.delete(`/admin/playlists/${row.id}`)
    ElMessage.success('删除成功'); loadPlaylists()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => loadPlaylists())
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 8px 12px; color: rgba(255,255,255,0.4); }
.search-box input { background: none; border: none; outline: none; color: #fff; font-size: 13px; width: 180px; }
.search-box input::placeholder { color: rgba(255,255,255,0.3); }
.filter-select { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 8px 12px; color: #fff; font-size: 13px; outline: none; }
.filter-select option { background: #1a1a2e; }
.data-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { padding: 14px 16px; text-align: left; font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.4); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.data-table td { padding: 12px 16px; font-size: 13px; color: rgba(255,255,255,0.7); border-bottom: 1px solid rgba(255,255,255,0.04); }
.data-table tr:hover td { background: rgba(255,255,255,0.02); }
.td-time { color: rgba(255,255,255,0.4); font-size: 12px; }
.pl-cover { width: 44px; height: 44px; border-radius: 8px; overflow: hidden; }
.pl-cover img { width: 100%; height: 100%; object-fit: cover; }
.pl-cover.placeholder { background: rgba(79,172,254,0.15); display: flex; align-items: center; justify-content: center; color: #4facfe; }
.pl-name-cell { display: flex; flex-direction: column; gap: 2px; }
.pl-name { color: rgba(255,255,255,0.85); font-weight: 500; }
.pl-desc { font-size: 11px; color: rgba(255,255,255,0.3); max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; }
.status-badge.active { background: rgba(67,233,123,0.15); color: #43e97b; }
.status-badge.disabled { background: rgba(245,87,108,0.15); color: #f5576c; }
.status-badge.private { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.4); }
.action-btns { display: flex; gap: 6px; }
.btn { padding: 6px 12px; border: none; border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-sm { padding: 4px 10px; }
.btn-primary { background: rgba(102,126,234,0.2); color: #667eea; }
.btn-primary:hover { background: rgba(102,126,234,0.3); }
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
