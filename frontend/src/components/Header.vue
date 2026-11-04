<template>
  <div class="header-wrapper">
  <header class="header">
    <div class="header-content">
      <div class="header-right">
        <div class="search-box" @click.stop>
          <div class="search-input-wrapper">
            <svg class="search-icon" viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
            <input
              v-model="keyword"
              placeholder="搜索歌曲、歌手、专辑"
              @keyup.enter="handleSearch"
              @focus="showDropdown = true"
              @blur="handleBlur"
              class="search-input"
            />
            <div class="search-shortcut" v-if="!keyword">Enter</div>
          </div>

          <div v-if="showDropdown" class="search-dropdown">
            <div v-if="hotSearch.length" class="dropdown-section">
              <div class="dropdown-title">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z"/></svg>
                热门搜索
              </div>
              <div class="hot-tags">
                <div
                  v-for="(tag, idx) in hotSearch"
                  :key="tag"
                  class="hot-tag"
                  @click="searchByTag(tag)"
                >
                  <span class="hot-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
                  {{ tag }}
                </div>
              </div>
            </div>
            <div v-if="searchHistory.length" class="dropdown-section">
              <div class="dropdown-title">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/></svg>
                搜索历史
                <div class="clear-btn" @click="handleClearHistory">清空</div>
              </div>
              <div class="history-list">
                <div
                  v-for="item in searchHistory"
                  :key="item"
                  class="history-item"
                  @click="searchByTag(item)"
                >
                  <span>{{ item }}</span>
                  <svg class="delete-icon" viewBox="0 0 24 24" fill="currentColor" width="12" height="12" @click.stop="handleDeleteHistory(item)"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="theme-toggle" @click="themeStore.toggleTheme">
          <svg v-if="themeStore.isDark" viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0 .39-.39.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-2.98 0-5.4-2.42-5.4-5.4 0-1.81.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/></svg>
        </div>
        <template v-if="userStore.isLoggedIn()">
          <div class="msg-icon" @click="$router.push('/messages')" title="私信">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/></svg>
            <span v-if="msgUnread > 0" class="msg-badge">{{ msgUnread > 99 ? '99+' : msgUnread }}</span>
          </div>
          <div class="notification-bell" @click="toggleNotificationPanel" :class="{ active: unreadCount > 0 }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z"/></svg>
            <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
          <div v-if="showNotificationPanel" class="notification-panel" @click.stop>
            <div class="notification-panel-header">
              <span>通知</span>
              <span v-if="unreadCount > 0" class="mark-all" @click="handleMarkAllRead">全部已读</span>
            </div>
            <div class="notification-list">
              <div v-if="notificationLoading" class="notification-loading">加载中...</div>
              <div v-else-if="!notifications.length" class="notification-empty">暂无通知</div>
              <div
                v-for="n in notifications"
                :key="n.id"
                :class="['notification-item', { unread: !n.isRead }]"
                @click="handleNotificationClick(n)"
              >
                <div v-if="n.type === 'announcement'" class="announcement-icon-wrap">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/></svg>
                </div>
                <el-avatar v-else :size="32" :src="n.actorAvatar">
                  {{ n.actorNickname?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="notification-content">
                  <div class="notification-text" :class="{ 'announcement-text': n.type === 'announcement' }">
                    <template v-if="n.type === 'announcement'">{{ n.content }}</template>
                    <template v-else>
                      <span class="notification-actor">{{ n.actorNickname }}</span>
                      {{ getNotificationAction(n.type) }}
                    </template>
                  </div>
                  <div class="notification-time">{{ formatNotificationTime(n.createTime) }}</div>
                </div>
                <div v-if="!n.isRead" class="notification-dot"></div>
              </div>
            </div>
          </div>
          <div class="user-info">
            <el-dropdown trigger="click">
              <div class="user-avatar">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                  {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="userStore.userInfo?.role === 'ADMIN'" @click="$router.push('/admin')">
                    <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" style="margin-right:6px"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/profile')">
                    <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" style="margin-right:6px"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/my-playlists')">
                    <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" style="margin-right:6px"><path d="M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z"/></svg>
                    我的歌单
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" style="margin-right:6px"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
        <template v-else>
          <button class="btn-login" @click="$router.push('/login')">登录</button>
          <button class="btn-register" @click="$router.push('/register')">注册</button>
        </template>
      </div>
    </div>
  </header>

  <div v-if="showAnnouncementDialog" class="announcement-dialog-overlay" @click.self="showAnnouncementDialog = false">
    <div class="announcement-dialog">
      <div class="announcement-dialog-header">
        <h3>{{ currentAnnouncement.title }}</h3>
        <button class="announcement-dialog-close" @click="showAnnouncementDialog = false">&times;</button>
      </div>
      <div class="announcement-dialog-meta">
        <span class="announcement-dialog-type" :class="currentAnnouncement.type">{{ announcementTypeMap[currentAnnouncement.type] || '系统公告' }}</span>
        <span class="announcement-dialog-time">{{ formatAnnouncementTime(currentAnnouncement.createTime) }}</span>
      </div>
      <div class="announcement-dialog-body">
        <p>{{ currentAnnouncement.content }}</p>
      </div>
    </div>
  </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { getSearchHistory, clearSearchHistory, deleteSearchHistory, getHotSearch, recordSearch } from '@/api/search'
import { getNotifications, getUnreadCount, markAllAsRead, markAsRead } from '@/api/notification'
import { getMessageUnreadCount } from '@/api/message'
import request, { setGuestMode } from '@/api/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const keyword = ref('')
const showDropdown = ref(false)
const searchHistory = ref([])
const hotSearch = ref([])
const showNotificationPanel = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const notificationLoading = ref(false)
let notificationTimer = null
const msgUnread = ref(0)
const showAnnouncementDialog = ref(false)
const currentAnnouncement = ref({})
const announcementTypeMap = { system: '系统公告', update: '版本更新', maintenance: '维护通知', activity: '活动通知' }

let clickTimer = null

onMounted(async () => {
  await loadHotSearch()
  if (userStore.userInfo?.userId) {
    await loadSearchHistory()
  }
  fetchUnreadCount()
  fetchMsgUnread()
  notificationTimer = setInterval(() => {
    fetchUnreadCount()
    fetchMsgUnread()
  }, 30000)
})

onUnmounted(() => {
  clearTimeout(clickTimer)
  clearInterval(notificationTimer)
})

const handleBlur = () => {
  clickTimer = setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

const loadHotSearch = async () => {
  try {
    const res = await getHotSearch(10)
    hotSearch.value = res.data || []
  } catch (e) {}
}

const loadSearchHistory = async () => {
  try {
    const res = await getSearchHistory(10)
    searchHistory.value = res.data || []
  } catch (e) {}
}

const handleSearch = () => {
  if (!keyword.value.trim()) return
  showDropdown.value = false
  if (userStore.userInfo?.userId) {
    recordSearch(keyword.value.trim()).catch(() => {})
  }
  router.push({ path: '/search', query: { keyword: keyword.value.trim() } })
}

const searchByTag = (tag) => {
  clearTimeout(clickTimer)
  keyword.value = tag
  handleSearch()
}

const handleDeleteHistory = async (item) => {
  try {
    await deleteSearchHistory(item)
    searchHistory.value = searchHistory.value.filter(h => h !== item)
  } catch (e) {}
}

const handleClearHistory = async () => {
  try {
    await clearSearchHistory()
    searchHistory.value = []
  } catch (e) {}
}

const handleLogout = () => {
  userStore.logout()
  setGuestMode(true)
  ElMessage.success('已退出登录')
  router.push('/')
}

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.count || 0
  } catch (e) {}
}

const fetchMsgUnread = async () => {
  try {
    const res = await getMessageUnreadCount()
    msgUnread.value = res.data?.count || 0
  } catch (e) {}
}

const toggleNotificationPanel = async () => {
  showNotificationPanel.value = !showNotificationPanel.value
  if (showNotificationPanel.value) {
    notificationLoading.value = true
    try {
      const res = await getNotifications()
      notifications.value = res.data || []
    } catch (e) {}
    finally {
      notificationLoading.value = false
    }
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
  } catch (e) {}
}

const handleNotificationClick = async (n) => {
  if (!n.isRead) {
    try {
      await markAsRead(n.id)
      n.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) {}
  }
  showNotificationPanel.value = false
  if (n.type === 'announcement') {
    await openAnnouncementDetail(n.targetId)
  } else if (n.type === 'FOLLOW') {
    router.push(`/user/${n.actorId}`)
  } else if (n.type === 'LIKE_POST' || n.type === 'COMMENT_POST') {
    router.push(`/community/post/${n.targetId}`)
  } else if (n.type === 'LIKE_COMMENT' || n.type === 'REPLY') {
    router.push('/messages')
  }
}

const openAnnouncementDetail = async (id) => {
  if (!id) return
  try {
    const res = await request.get(`/announcements/${id}`)
    if (res.code === 200 && res.data) {
      currentAnnouncement.value = res.data
      showAnnouncementDialog.value = true
    }
  } catch (e) {
    console.error(e)
  }
}

const formatAnnouncementTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

const getNotificationAction = (type) => {
  const map = {
    FOLLOW: '关注了你',
    REPLY: '回复了你的评论',
    LIKE_POST: '赞了你的帖子',
    COMMENT_POST: '评论了你的帖子',
    LIKE_COMMENT: '赞了你的评论',
  }
  return map[type] || ''
}

const formatNotificationTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = (now - date) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return date.toLocaleDateString()
}
</script>

<style scoped>
.header {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  height: 56px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-box {
  position: relative;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 0 14px;
  height: 36px;
  width: 260px;
  transition: all 0.3s;
}

.search-input-wrapper:focus-within {
  border-color: var(--primary-color);
  box-shadow: var(--neon-glow-sm);
}

.search-icon {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.search-shortcut {
  font-size: 10px;
  color: var(--text-tertiary);
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 1px 6px;
  flex-shrink: 0;
}

.search-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 320px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  box-shadow: var(--card-hover-shadow);
  z-index: 200;
  padding: 16px;
  backdrop-filter: blur(20px);
}

.dropdown-section {
  margin-bottom: 16px;
}

.dropdown-section:last-child {
  margin-bottom: 0;
}

.dropdown-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 12px;
  letter-spacing: 0.5px;
}

.clear-btn {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color 0.2s;
  font-weight: 400;
}

.clear-btn:hover {
  color: var(--primary-color);
}

.hot-tags {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hot-tag {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.hot-tag:hover {
  background: var(--nav-hover-bg);
  color: var(--text-primary);
}

.hot-rank {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.hot-rank.top {
  color: var(--accent-cyan);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  transition: background 0.2s;
}

.history-item:hover {
  background: var(--nav-hover-bg);
}

.history-item .delete-icon {
  margin-left: auto;
  color: var(--text-tertiary);
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
}

.history-item:hover .delete-icon {
  opacity: 1;
}

.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
  border: 1px solid var(--border-color);
}

.theme-toggle:hover {
  background: var(--nav-hover-bg);
  color: var(--primary-color);
  border-color: var(--border-glow);
}

.msg-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
  border: 1px solid var(--border-color);
  position: relative;
}

.msg-icon:hover {
  background: var(--nav-hover-bg);
  color: var(--primary-color);
  border-color: var(--border-glow);
}

.msg-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: #ef4444;
  border-radius: 8px;
  text-align: center;
}

.notification-bell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
  border: 1px solid var(--border-color);
  position: relative;
}

.notification-bell:hover {
  background: var(--nav-hover-bg);
  color: var(--primary-color);
  border-color: var(--border-glow);
}

.notification-bell.active {
  color: var(--accent-amber);
}

.notification-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: #ef4444;
  border-radius: 8px;
  text-align: center;
}

.notification-panel {
  position: absolute;
  top: 56px;
  right: 60px;
  width: 360px;
  max-height: 480px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: var(--card-hover-shadow);
  z-index: 200;
  overflow: hidden;
}

.notification-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.mark-all {
  font-size: 12px;
  font-weight: 400;
  color: var(--primary-color);
  cursor: pointer;
}

.mark-all:hover {
  text-decoration: underline;
}

.notification-list {
  max-height: 420px;
  overflow-y: auto;
}

.notification-loading,
.notification-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--text-tertiary);
  font-size: 13px;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.notification-item:hover {
  background: var(--nav-hover-bg);
}

.notification-item.unread {
  background: rgba(99, 102, 241, 0.04);
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.4;
}

.notification-actor {
  font-weight: 600;
  color: var(--primary-color);
}

.notification-time {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  flex-shrink: 0;
}

.user-avatar {
  cursor: pointer;
  border-radius: 50%;
  transition: box-shadow 0.2s;
}

.user-avatar:hover {
  box-shadow: var(--neon-glow-sm);
}

.announcement-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.btn-login {
  padding: 6px 16px;
  border-radius: 8px;
  border: 1px solid var(--primary-color);
  background: transparent;
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-login:hover {
  background: var(--primary-color);
  color: #fff;
}

.btn-register {
  padding: 6px 16px;
  border-radius: 8px;
  border: none;
  background: var(--gradient-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-register:hover {
  opacity: 0.9;
  box-shadow: var(--neon-glow-sm);
}

.announcement-text {
  color: var(--text-primary);
  font-weight: 500;
}

.announcement-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.announcement-dialog {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  width: 520px;
  max-width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}

.announcement-dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px 24px 12px;
}

.announcement-dialog-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  line-height: 1.4;
  flex: 1;
  padding-right: 16px;
}

.announcement-dialog-close {
  background: none;
  border: none;
  color: var(--text-tertiary);
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}

.announcement-dialog-close:hover {
  color: var(--text-primary);
}

.announcement-dialog-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px 16px;
}

.announcement-dialog-type {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.announcement-dialog-type.system {
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
}

.announcement-dialog-type.update {
  background: rgba(67, 233, 123, 0.15);
  color: #43e97b;
}

.announcement-dialog-type.maintenance {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.announcement-dialog-type.activity {
  background: rgba(240, 147, 251, 0.15);
  color: #f093fb;
}

.announcement-dialog-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.announcement-dialog-body {
  padding: 0 24px 24px;
}

.announcement-dialog-body p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
