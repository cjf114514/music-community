<template>
  <div class="announcement-manage">
    <div class="page-header">
      <h2>公告管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="openCreateDialog">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
          发布公告
        </button>
      </div>
    </div>

    <div class="stats-row">
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
          <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/></svg>
        </div>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ pagination.total }}</span>
          <span class="mini-stat-label">公告总数</span>
        </div>
      </div>
    </div>

    <div class="announcement-list">
      <div class="announcement-card" v-for="item in announcements" :key="item.id">
        <div class="announcement-header">
          <div class="announcement-title-row">
            <span class="top-badge" v-if="item.isTop">置顶</span>
            <h4 class="announcement-title">{{ item.title }}</h4>
            <span class="status-tag" :class="item.status">{{ item.status === 'published' ? '已发布' : '草稿' }}</span>
          </div>
          <div class="announcement-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              {{ item.creatorName || '管理员' }}
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>
              {{ formatTime(item.createTime) }}
            </span>
            <span class="meta-item type-badge" :class="item.type">{{ typeMap[item.type] || item.type }}</span>
          </div>
        </div>
        <div class="announcement-body">
          <p>{{ item.content }}</p>
        </div>
        <div class="announcement-footer">
          <button class="btn btn-sm btn-ghost" @click="openEditDialog(item)">编辑</button>
          <button class="btn btn-sm btn-ghost" @click="toggleStatus(item)">
            {{ item.status === 'published' ? '转为草稿' : '发布' }}
          </button>
          <button class="btn btn-sm btn-danger-outline" @click="deleteAnnouncement(item)">删除</button>
        </div>
      </div>

      <div class="empty-state" v-if="announcements.length === 0">
        <svg viewBox="0 0 24 24" fill="currentColor" width="48" height="48"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/></svg>
        <p>暂无公告</p>
      </div>
    </div>

    <div class="pagination" v-if="pagination.total > pagination.size">
      <button class="btn btn-sm btn-ghost" :disabled="pagination.page === 0" @click="loadAnnouncements(pagination.page - 1)">上一页</button>
      <span class="page-info">{{ pagination.page + 1 }} / {{ totalPages }}</span>
      <button class="btn btn-sm btn-ghost" :disabled="pagination.page >= totalPages - 1" @click="loadAnnouncements(pagination.page + 1)">下一页</button>
    </div>

    <div class="modal-overlay" v-if="showDialog" @click.self="closeDialog">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ isEditing ? '编辑公告' : '发布公告' }}</h3>
          <button class="modal-close" @click="closeDialog">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>标题</label>
            <input v-model="form.title" placeholder="请输入公告标题" />
          </div>
          <div class="form-group">
            <label>内容</label>
            <textarea v-model="form.content" placeholder="请输入公告内容" rows="6"></textarea>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>类型</label>
              <select v-model="form.type">
                <option value="system">系统公告</option>
                <option value="update">版本更新</option>
                <option value="maintenance">维护通知</option>
                <option value="activity">活动通知</option>
              </select>
            </div>
            <div class="form-group">
              <label>置顶</label>
              <label class="toggle">
                <input type="checkbox" v-model="form.isTop" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="closeDialog">取消</button>
          <button class="btn btn-primary" @click="submitForm">{{ isEditing ? '保存' : '发布' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const announcements = ref([])
const showDialog = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const typeMap = { system: '系统公告', update: '版本更新', maintenance: '维护通知', activity: '活动通知' }

const pagination = reactive({ page: 0, size: 20, total: 0 })
const totalPages = computed(() => Math.ceil(pagination.total / pagination.size))

const form = reactive({
  title: '',
  content: '',
  type: 'system',
  isTop: false
})

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

const loadAnnouncements = async (page = 0) => {
  try {
    const res = await request.get('/admin/announcements', { params: { page, size: pagination.size } })
    if (res.code === 200) {
      announcements.value = res.data.list || []
      pagination.total = res.data.total || 0
      pagination.page = page
    }
  } catch (e) {
    console.error(e)
  }
}

const openCreateDialog = () => {
  isEditing.value = false
  editingId.value = null
  form.title = ''
  form.content = ''
  form.type = 'system'
  form.isTop = false
  showDialog.value = true
}

const openEditDialog = (item) => {
  isEditing.value = true
  editingId.value = item.id
  form.title = item.title
  form.content = item.content
  form.type = item.type
  form.isTop = item.isTop
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const submitForm = async () => {
  if (!form.title.trim()) { ElMessage.warning('请输入标题'); return }
  if (!form.content.trim()) { ElMessage.warning('请输入内容'); return }
  try {
    if (isEditing.value) {
      await request.put(`/admin/announcements/${editingId.value}`, {
        title: form.title, content: form.content, type: form.type, isTop: form.isTop
      })
      ElMessage.success('公告已更新')
    } else {
      await request.post('/admin/announcements', {
        title: form.title, content: form.content, type: form.type, isTop: form.isTop
      })
      ElMessage.success('公告已发布')
    }
    closeDialog()
    loadAnnouncements(pagination.page)
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const toggleStatus = async (item) => {
  try {
    await request.put(`/admin/announcements/${item.id}/toggle-status`)
    ElMessage.success('状态已切换')
    loadAnnouncements(pagination.page)
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteAnnouncement = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除公告「${item.title}」吗？`, '删除确认', { type: 'warning' })
    await request.delete(`/admin/announcements/${item.id}`)
    ElMessage.success('公告已删除')
    loadAnnouncements(pagination.page)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => loadAnnouncements())
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; margin: 0; }

.stats-row { display: flex; gap: 16px; margin-bottom: 20px; }
.mini-stat { display: flex; align-items: center; gap: 12px; background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; padding: 14px 18px; }
.mini-stat-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.mini-stat-info { display: flex; flex-direction: column; }
.mini-stat-value { font-size: 20px; font-weight: 700; color: #fff; }
.mini-stat-label { font-size: 12px; color: rgba(255,255,255,0.4); }

.announcement-list { display: flex; flex-direction: column; gap: 12px; }
.announcement-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 20px; transition: all 0.3s; }
.announcement-card:hover { border-color: rgba(102,126,234,0.2); }

.announcement-header { margin-bottom: 12px; }
.announcement-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.announcement-title { font-size: 15px; font-weight: 600; color: #fff; margin: 0; flex: 1; }
.top-badge { background: rgba(245,158,11,0.15); color: #f59e0b; font-size: 11px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.status-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.status-tag.published { background: rgba(67,233,123,0.15); color: #43e97b; }
.status-tag.draft { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.4); }

.announcement-meta { display: flex; align-items: center; gap: 16px; }
.meta-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: rgba(255,255,255,0.4); }
.type-badge { padding: 1px 6px; border-radius: 3px; font-size: 11px; }
.type-badge.system { background: rgba(102,126,234,0.15); color: #667eea; }
.type-badge.update { background: rgba(67,233,123,0.15); color: #43e97b; }
.type-badge.maintenance { background: rgba(245,158,11,0.15); color: #f59e0b; }
.type-badge.activity { background: rgba(240,147,251,0.15); color: #f093fb; }

.announcement-body { margin-bottom: 16px; }
.announcement-body p { font-size: 13px; color: rgba(255,255,255,0.6); line-height: 1.6; margin: 0; }

.announcement-footer { display: flex; gap: 8px; }

.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: rgba(255,255,255,0.2); }
.empty-state p { margin-top: 12px; font-size: 14px; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 20px; }
.page-info { font-size: 13px; color: rgba(255,255,255,0.5); }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: #1a1a2e; border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; width: 520px; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.modal-header h3 { font-size: 16px; font-weight: 600; color: #fff; margin: 0; }
.modal-close { background: none; border: none; color: rgba(255,255,255,0.4); font-size: 20px; cursor: pointer; }
.modal-body { padding: 20px 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 16px 24px; border-top: 1px solid rgba(255,255,255,0.06); }

.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; color: rgba(255,255,255,0.6); margin-bottom: 6px; }
.form-group input, .form-group textarea, .form-group select { width: 100%; padding: 10px 12px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; color: #fff; font-size: 13px; outline: none; transition: border-color 0.2s; box-sizing: border-box; }
.form-group input:focus, .form-group textarea:focus, .form-group select:focus { border-color: rgba(102,126,234,0.5); }
.form-group textarea { resize: vertical; min-height: 120px; }
.form-group select option { background: #1a1a2e; }
.form-row { display: flex; gap: 16px; }
.form-row .form-group { flex: 1; }

.toggle { position: relative; display: inline-block; width: 40px; height: 22px; cursor: pointer; }
.toggle input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.1); border-radius: 22px; transition: 0.3s; }
.toggle-slider::before { content: ''; position: absolute; height: 16px; width: 16px; left: 3px; bottom: 3px; background: rgba(255,255,255,0.6); border-radius: 50%; transition: 0.3s; }
.toggle input:checked + .toggle-slider { background: rgba(102,126,234,0.4); }
.toggle input:checked + .toggle-slider::before { transform: translateX(18px); background: #667eea; }

.btn { padding: 8px 16px; border: none; border-radius: 8px; font-size: 13px; cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px; }
.btn-sm { padding: 5px 12px; font-size: 12px; }
.btn-primary { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.btn-primary:hover { opacity: 0.9; }
.btn-ghost { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.6); }
.btn-ghost:hover { background: rgba(255,255,255,0.1); }
.btn-ghost:disabled { opacity: 0.3; cursor: not-allowed; }
.btn-danger-outline { background: rgba(245,87,108,0.1); color: #f5576c; }
.btn-danger-outline:hover { background: rgba(245,87,108,0.2); }
</style>
