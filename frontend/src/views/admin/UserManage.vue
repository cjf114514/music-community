<template>
  <div class="user-manage">
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="header-actions">
        <div class="search-box">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
          <input v-model="searchForm.keyword" placeholder="搜索用户名或昵称" @keyup.enter="loadUsers" />
        </div>
        <select v-model="searchForm.role" class="filter-select" @change="loadUsers">
          <option value="">全部角色</option>
          <option value="ADMIN">管理员</option>
          <option value="USER">普通用户</option>
        </select>
      </div>
    </div>

    <div class="data-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户</th>
            <th>邮箱</th>
            <th>性别</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td class="td-id">{{ user.id }}</td>
            <td>
              <div class="user-cell">
                <div class="user-avatar" v-if="user.avatar">
                  <img :src="user.avatar" alt="" />
                </div>
                <div class="user-avatar placeholder" v-else>
                  <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                </div>
                <div class="user-text">
                  <span class="user-nickname">{{ user.nickname || user.username }}</span>
                  <span class="user-username">@{{ user.username }}</span>
                </div>
              </div>
            </td>
            <td class="td-email">{{ user.email || '-' }}</td>
            <td>
              <span class="gender-tag" :class="user.gender === 1 ? 'male' : user.gender === 2 ? 'female' : ''">
                {{ user.gender === 1 ? '男' : user.gender === 2 ? '女' : '未知' }}
              </span>
            </td>
            <td>
              <span class="role-tag" :class="user.role === 'ADMIN' ? 'admin' : 'user'">
                {{ user.role === 'ADMIN' ? '管理员' : '用户' }}
              </span>
            </td>
            <td>
              <span class="status-dot" :class="user.status === 1 ? 'active' : 'disabled'"></span>
              {{ user.status === 1 ? '正常' : '禁用' }}
            </td>
            <td class="td-time">{{ formatTime(user.createTime) }}</td>
            <td>
              <div class="action-btns">
                <button class="btn btn-sm" :class="user.role === 'ADMIN' ? 'btn-warning' : 'btn-primary'" @click="changeRole(user)">
                  {{ user.role === 'ADMIN' ? '取消管理员' : '设为管理员' }}
                </button>
                <button class="btn btn-sm" :class="user.status === 1 ? 'btn-danger' : 'btn-success'" @click="changeStatus(user)">
                  {{ user.status === 1 ? '禁用' : '启用' }}
                </button>
                <button class="btn btn-sm btn-danger-outline" @click="deleteUser(user)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span class="total-text">共 {{ pagination.total }} 条</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="pagination.page <= 0" @click="pagination.page--; loadUsers()">上一页</button>
          <span class="page-info">{{ pagination.page + 1 }} / {{ Math.ceil(pagination.total / pagination.size) || 1 }}</span>
          <button class="page-btn" :disabled="(pagination.page + 1) * pagination.size >= pagination.total" @click="pagination.page++; loadUsers()">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const users = ref([])
const searchForm = reactive({ keyword: '', role: '' })
const pagination = reactive({ page: 0, size: 20, total: 0 })

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

const loadUsers = async () => {
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.role) params.role = searchForm.role
    const res = await request.get('/admin/users', { params })
    if (res.code === 200) {
      users.value = res.data.list
      pagination.total = res.data.total
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const changeRole = async (user) => {
  const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  const action = newRole === 'ADMIN' ? '设为管理员' : '取消管理员'
  try {
    await ElMessageBox.confirm(`确定要将 ${user.nickname || user.username} ${action}吗？`, '提示', { type: 'warning' })
    await request.put(`/admin/users/${user.id}/role`, { role: newRole })
    ElMessage.success('操作成功')
    loadUsers()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const changeStatus = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 ${user.nickname || user.username} 吗？`, '提示', { type: 'warning' })
    await request.put(`/admin/users/${user.id}/status`, { status: newStatus })
    ElMessage.success('操作成功')
    loadUsers()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 ${user.nickname || user.username} 吗？此操作不可恢复！`, '警告', { type: 'error' })
    await request.delete(`/admin/users/${user.id}`)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => loadUsers())
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  padding: 8px 12px;
  color: rgba(255,255,255,0.4);
}

.search-box input {
  background: none;
  border: none;
  outline: none;
  color: #fff;
  font-size: 13px;
  width: 180px;
}

.search-box input::placeholder { color: rgba(255,255,255,0.3); }

.filter-select {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  padding: 8px 12px;
  color: #fff;
  font-size: 13px;
  outline: none;
}

.filter-select option { background: #1a1a2e; }

.data-card {
  background: rgba(26, 26, 46, 0.6);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  padding: 14px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,0.4);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.data-table td {
  padding: 12px 16px;
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.data-table tr:hover td { background: rgba(255,255,255,0.02); }

.td-id { color: rgba(255,255,255,0.3); font-size: 12px; }
.td-email { color: rgba(255,255,255,0.5); }
.td-time { color: rgba(255,255,255,0.4); font-size: 12px; }

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.user-avatar img { width: 100%; height: 100%; object-fit: cover; }

.user-avatar.placeholder {
  background: rgba(102,126,234,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
}

.user-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-nickname { color: rgba(255,255,255,0.8); font-weight: 500; }
.user-username { color: rgba(255,255,255,0.3); font-size: 11px; }

.gender-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.5);
}

.gender-tag.male { background: rgba(79,172,254,0.15); color: #4facfe; }
.gender-tag.female { background: rgba(245,87,108,0.15); color: #f5576c; }

.role-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.role-tag.admin { background: rgba(102,126,234,0.15); color: #667eea; }
.role-tag.user { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.5); }

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}

.status-dot.active { background: #43e97b; }
.status-dot.disabled { background: #f5576c; }

.action-btns {
  display: flex;
  gap: 6px;
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-sm { padding: 4px 10px; }
.btn-primary { background: rgba(102,126,234,0.2); color: #667eea; }
.btn-primary:hover { background: rgba(102,126,234,0.3); }
.btn-warning { background: rgba(255,170,0,0.15); color: #faa; }
.btn-warning:hover { background: rgba(255,170,0,0.25); }
.btn-danger { background: rgba(245,87,108,0.15); color: #f5576c; }
.btn-danger:hover { background: rgba(245,87,108,0.25); }
.btn-danger-outline { background: transparent; color: #f5576c; border: 1px solid rgba(245,87,108,0.3); }
.btn-danger-outline:hover { background: rgba(245,87,108,0.1); }
.btn-success { background: rgba(67,233,123,0.15); color: #43e97b; }
.btn-success:hover { background: rgba(67,233,123,0.25); }

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}

.total-text { font-size: 13px; color: rgba(255,255,255,0.4); }

.page-btns {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-btn {
  padding: 6px 14px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 6px;
  color: rgba(255,255,255,0.6);
  font-size: 13px;
  cursor: pointer;
}

.page-btn:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.page-info { font-size: 13px; color: rgba(255,255,255,0.5); }
</style>
