<template>
  <div class="system-settings">
    <div class="page-header">
      <h2>系统设置</h2>
    </div>

    <div class="settings-grid">
      <div class="settings-card">
        <div class="card-header">
          <div class="card-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          </div>
          <div>
            <h3>管理员账号</h3>
            <p>当前登录的管理员信息</p>
          </div>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ adminInfo.username }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ adminInfo.nickname || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">角色</span>
            <span class="role-tag admin">管理员</span>
          </div>
          <div class="info-row">
            <span class="info-label">注册时间</span>
            <span class="info-value">{{ formatTime(adminInfo.createTime) }}</span>
          </div>
        </div>
      </div>

      <div class="settings-card">
        <div class="card-header">
          <div class="card-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
          </div>
          <div>
            <h3>内容统计</h3>
            <p>平台内容数据概览</p>
          </div>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="info-label">歌曲总数</span>
            <span class="info-value highlight">{{ stats.songCount }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">歌手总数</span>
            <span class="info-value highlight">{{ stats.singerCount }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">歌单总数</span>
            <span class="info-value highlight">{{ stats.playlistCount }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">评论总数</span>
            <span class="info-value highlight">{{ stats.commentCount }}</span>
          </div>
        </div>
      </div>

      <div class="settings-card">
        <div class="card-header">
          <div class="card-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>
          </div>
          <div>
            <h3>系统配置</h3>
            <p>平台运行参数设置</p>
          </div>
        </div>
        <div class="card-body">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">用户注册</span>
              <span class="setting-desc">允许新用户注册账号</span>
            </div>
            <label class="toggle">
              <input type="checkbox" v-model="settings.allowRegister" @change="saveSettings" />
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">评论审核</span>
              <span class="setting-desc">新评论需要管理员审核后显示</span>
            </div>
            <label class="toggle">
              <input type="checkbox" v-model="settings.commentReview" @change="saveSettings" />
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">社区功能</span>
              <span class="setting-desc">开启社区帖子发布功能</span>
            </div>
            <label class="toggle">
              <input type="checkbox" v-model="settings.communityEnabled" @change="saveSettings" />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
      </div>

      <div class="settings-card">
        <div class="card-header">
          <div class="card-icon" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          </div>
          <div>
            <h3>危险操作</h3>
            <p>不可逆的系统操作</p>
          </div>
        </div>
        <div class="card-body">
          <div class="danger-item">
            <div class="danger-info">
              <span class="danger-name">清除播放记录</span>
              <span class="danger-desc">清除所有用户的播放历史记录</span>
            </div>
            <button class="btn btn-danger" @click="clearPlayHistory">执行</button>
          </div>
          <div class="danger-item">
            <div class="danger-info">
              <span class="danger-name">清除搜索记录</span>
              <span class="danger-desc">清除所有用户的搜索历史</span>
            </div>
            <button class="btn btn-danger" @click="clearSearchHistory">执行</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const adminInfo = ref({})
const stats = reactive({
  songCount: 0,
  singerCount: 0,
  playlistCount: 0,
  commentCount: 0
})
const settings = reactive({
  allowRegister: true,
  commentReview: false,
  communityEnabled: true
})

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

const loadAdminInfo = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  adminInfo.value = user
}

const loadStats = async () => {
  try {
    const res = await request.get('/admin/dashboard')
    if (res.code === 200) {
      stats.songCount = res.data.songCount || 0
      stats.singerCount = res.data.singerCount || 0
      stats.playlistCount = res.data.playlistCount || 0
      stats.commentCount = res.data.commentCount || 0
    }
  } catch (e) {
    console.error(e)
  }
}

const loadSettings = async () => {
  try {
    const res = await request.get('/admin/settings')
    if (res.code === 200) {
      settings.allowRegister = res.data.allowRegister
      settings.commentReview = res.data.commentReview
      settings.communityEnabled = res.data.communityEnabled
    }
  } catch (e) {
    console.error(e)
  }
}

const saveSettings = async () => {
  try {
    await request.put('/admin/settings', {
      allowRegister: settings.allowRegister,
      commentReview: settings.commentReview,
      communityEnabled: settings.communityEnabled
    })
    ElMessage.success('设置已保存')
  } catch (e) {
    ElMessage.error('保存设置失败')
  }
}

const clearPlayHistory = async () => {
  try {
    await ElMessageBox.confirm('确定清除所有播放记录吗？此操作不可恢复！', '危险操作', { type: 'error' })
    await request.delete('/admin/settings/play-history')
    ElMessage.success('播放记录已清除')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const clearSearchHistory = async () => {
  try {
    await ElMessageBox.confirm('确定清除所有搜索记录吗？此操作不可恢复！', '危险操作', { type: 'error' })
    await request.delete('/admin/settings/search-history')
    ElMessage.success('搜索记录已清除')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadAdminInfo()
  loadStats()
  loadSettings()
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; }

.settings-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }

.settings-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; transition: all 0.3s; }
.settings-card:hover { border-color: rgba(102,126,234,0.2); }

.card-header { display: flex; align-items: center; gap: 14px; padding: 20px 20px 0; }
.card-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.card-header h3 { font-size: 15px; font-weight: 600; color: #fff; margin: 0; }
.card-header p { font-size: 12px; color: rgba(255,255,255,0.4); margin: 4px 0 0; }

.card-body { padding: 16px 20px 20px; }

.info-row { display: flex; align-items: center; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 13px; color: rgba(255,255,255,0.5); }
.info-value { font-size: 13px; color: rgba(255,255,255,0.8); }
.info-value.highlight { color: #667eea; font-weight: 600; font-size: 16px; }

.role-tag { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 500; }
.role-tag.admin { background: rgba(102,126,234,0.15); color: #667eea; }

.setting-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
.setting-item:last-child { border-bottom: none; }
.setting-info { display: flex; flex-direction: column; gap: 2px; }
.setting-name { font-size: 13px; color: rgba(255,255,255,0.8); font-weight: 500; }
.setting-desc { font-size: 11px; color: rgba(255,255,255,0.3); }

.toggle { position: relative; display: inline-block; width: 40px; height: 22px; cursor: pointer; }
.toggle input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.1); border-radius: 22px; transition: 0.3s; }
.toggle-slider::before { content: ''; position: absolute; height: 16px; width: 16px; left: 3px; bottom: 3px; background: rgba(255,255,255,0.6); border-radius: 50%; transition: 0.3s; }
.toggle input:checked + .toggle-slider { background: rgba(102,126,234,0.4); }
.toggle input:checked + .toggle-slider::before { transform: translateX(18px); background: #667eea; }

.danger-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
.danger-item:last-child { border-bottom: none; }
.danger-info { display: flex; flex-direction: column; gap: 2px; }
.danger-name { font-size: 13px; color: rgba(255,255,255,0.8); font-weight: 500; }
.danger-desc { font-size: 11px; color: rgba(255,255,255,0.3); }

.btn { padding: 6px 16px; border: none; border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-danger { background: rgba(245,87,108,0.15); color: #f5576c; }
.btn-danger:hover { background: rgba(245,87,108,0.25); }
</style>
