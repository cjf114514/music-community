<template>
  <div class="operation-log">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>

    <div class="filter-bar">
      <div class="filter-group">
        <label>模块</label>
        <select v-model="filters.module" @change="loadLogs">
          <option value="">全部</option>
          <option value="用户管理">用户管理</option>
          <option value="歌曲管理">歌曲管理</option>
          <option value="歌单管理">歌单管理</option>
          <option value="评论管理">评论管理</option>
          <option value="社区管理">社区管理</option>
          <option value="公告管理">公告管理</option>
          <option value="系统设置">系统设置</option>
        </select>
      </div>
      <div class="filter-group">
        <label>操作</label>
        <select v-model="filters.action" @change="loadLogs">
          <option value="">全部</option>
          <option value="CREATE">创建</option>
          <option value="UPDATE">修改</option>
          <option value="DELETE">删除</option>
        </select>
      </div>
      <div class="filter-group">
        <label>搜索</label>
        <input v-model="filters.keyword" placeholder="搜索操作人或描述" @keyup.enter="loadLogs" />
      </div>
      <button class="btn btn-primary btn-sm" @click="loadLogs">查询</button>
    </div>

    <div class="log-table">
      <table>
        <thead>
          <tr>
            <th>时间</th>
            <th>操作人</th>
            <th>模块</th>
            <th>操作</th>
            <th>描述</th>
            <th>目标类型</th>
            <th>目标ID</th>
            <th>IP</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td class="time-cell">{{ formatTime(log.createTime) }}</td>
            <td>{{ log.username || '-' }}</td>
            <td><span class="module-tag">{{ log.module }}</span></td>
            <td><span class="action-tag" :class="log.action?.toLowerCase()">{{ actionMap[log.action] || log.action }}</span></td>
            <td class="desc-cell">{{ log.description }}</td>
            <td>{{ log.targetType || '-' }}</td>
            <td>{{ log.targetId || '-' }}</td>
            <td class="ip-cell">{{ log.ip || '-' }}</td>
          </tr>
          <tr v-if="logs.length === 0">
            <td colspan="8" class="empty-cell">暂无操作日志</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="pagination.total > pagination.size">
      <button class="btn btn-sm btn-ghost" :disabled="pagination.page === 0" @click="loadLogs(pagination.page - 1)">上一页</button>
      <span class="page-info">{{ pagination.page + 1 }} / {{ totalPages }}</span>
      <button class="btn btn-sm btn-ghost" :disabled="pagination.page >= totalPages - 1" @click="loadLogs(pagination.page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/api/request'

const logs = ref([])
const actionMap = { CREATE: '创建', UPDATE: '修改', DELETE: '删除' }

const filters = reactive({ module: '', action: '', keyword: '' })
const pagination = reactive({ page: 0, size: 20, total: 0 })
const totalPages = computed(() => Math.ceil(pagination.total / pagination.size))

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

const loadLogs = async (page = 0) => {
  try {
    const params = { page, size: pagination.size }
    if (filters.module) params.module = filters.module
    if (filters.action) params.action = filters.action
    if (filters.keyword) params.keyword = filters.keyword
    const res = await request.get('/admin/operation-logs', { params })
    if (res.code === 200) {
      logs.value = res.data.list || []
      pagination.total = res.data.total || 0
      pagination.page = page
    }
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => loadLogs())
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; }

.filter-bar { display: flex; align-items: flex-end; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.filter-group { display: flex; flex-direction: column; gap: 4px; }
.filter-group label { font-size: 11px; color: rgba(255,255,255,0.4); }
.filter-group input, .filter-group select { padding: 8px 12px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; color: #fff; font-size: 13px; outline: none; min-width: 140px; }
.filter-group input:focus, .filter-group select:focus { border-color: rgba(102,126,234,0.5); }
.filter-group select option { background: #1a1a2e; }

.log-table { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; }
.log-table table { width: 100%; border-collapse: collapse; }
.log-table th { padding: 12px 16px; text-align: left; font-size: 12px; color: rgba(255,255,255,0.4); font-weight: 500; border-bottom: 1px solid rgba(255,255,255,0.06); white-space: nowrap; }
.log-table td { padding: 10px 16px; font-size: 13px; color: rgba(255,255,255,0.7); border-bottom: 1px solid rgba(255,255,255,0.03); }
.log-table tr:hover td { background: rgba(255,255,255,0.02); }

.time-cell { white-space: nowrap; font-size: 12px; color: rgba(255,255,255,0.5); }
.desc-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ip-cell { font-size: 12px; color: rgba(255,255,255,0.4); font-family: monospace; }
.empty-cell { text-align: center; color: rgba(255,255,255,0.2); padding: 40px 0 !important; }

.module-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; background: rgba(102,126,234,0.12); color: #667eea; }
.action-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.action-tag.create { background: rgba(67,233,123,0.12); color: #43e97b; }
.action-tag.update { background: rgba(79,172,254,0.12); color: #4facfe; }
.action-tag.delete { background: rgba(245,87,108,0.12); color: #f5576c; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 20px; }
.page-info { font-size: 13px; color: rgba(255,255,255,0.5); }

.btn { padding: 8px 16px; border: none; border-radius: 8px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.btn-sm { padding: 6px 12px; font-size: 12px; }
.btn-primary { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.btn-primary:hover { opacity: 0.9; }
.btn-ghost { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.6); }
.btn-ghost:hover { background: rgba(255,255,255,0.1); }
.btn-ghost:disabled { opacity: 0.3; cursor: not-allowed; }
</style>
