<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
            <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
          </svg>
        </div>
        <span class="logo-text">MusicAdmin</span>
      </div>

      <div class="sidebar-menu">
        <div class="menu-group">
          <div class="menu-group-title">概览</div>
          <router-link to="/admin/dashboard" class="menu-item" :class="{ active: route.path === '/admin/dashboard' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>
            <span>数据看板</span>
          </router-link>
        </div>

        <div class="menu-group">
          <div class="menu-group-title">内容管理</div>
          <router-link to="/admin/songs" class="menu-item" :class="{ active: route.path === '/admin/songs' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
            <span>歌曲管理</span>
          </router-link>
          <router-link to="/admin/batch-import" class="menu-item" :class="{ active: route.path === '/admin/batch-import' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/></svg>
            <span>批量导入</span>
          </router-link>
          <router-link to="/admin/library" class="menu-item" :class="{ active: route.path === '/admin/library' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg>
            <span>音乐库扫描</span>
          </router-link>
          <router-link to="/admin/playlists" class="menu-item" :class="{ active: route.path === '/admin/playlists' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z"/></svg>
            <span>歌单管理</span>
          </router-link>
        </div>

        <div class="menu-group">
          <div class="menu-group-title">社区运营</div>
          <router-link to="/admin/comments" class="menu-item" :class="{ active: route.path === '/admin/comments' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M21 6h-2v9H6v2c0 .55.45 1 1 1h11l4 4V7c0-.55-.45-1-1-1zm-4 6V3c0-.55-.45-1-1-1H3c-.55 0-1 .45-1 1v14l4-4h10c.55 0 1-.45 1-1z"/></svg>
            <span>评论管理</span>
          </router-link>
          <router-link to="/admin/community" class="menu-item" :class="{ active: route.path === '/admin/community' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
            <span>社区管理</span>
          </router-link>
          <router-link to="/admin/announcements" class="menu-item" :class="{ active: route.path === '/admin/announcements' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/></svg>
            <span>公告管理</span>
          </router-link>
        </div>

        <div class="menu-group">
          <div class="menu-group-title">系统</div>
          <router-link to="/admin/users" class="menu-item" :class="{ active: route.path === '/admin/users' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <span>用户管理</span>
          </router-link>
          <router-link to="/admin/operation-logs" class="menu-item" :class="{ active: route.path === '/admin/operation-logs' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>
            <span>操作日志</span>
          </router-link>
          <router-link to="/admin/settings" class="menu-item" :class="{ active: route.path === '/admin/settings' }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>
            <span>系统设置</span>
          </router-link>
        </div>
      </div>

      <div class="sidebar-footer">
        <div class="back-link" @click="$router.push('/')">
          <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
          <span>返回前台</span>
        </div>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div class="breadcrumb">
          <span class="breadcrumb-item" @click="$router.push('/admin/dashboard')">首页</span>
          <span class="breadcrumb-sep" v-if="currentPageTitle">/</span>
          <span class="breadcrumb-current" v-if="currentPageTitle">{{ currentPageTitle }}</span>
        </div>
        <div class="topbar-right">
          <div class="admin-info">
            <div class="admin-avatar">
              <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <span class="admin-name">{{ adminName }}</span>
            <el-tag size="small" type="danger" effect="dark">管理员</el-tag>
          </div>
        </div>
      </header>

      <main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const adminName = computed(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.nickname || user.username || '管理员'
})

const currentPageTitle = computed(() => {
  const map = {
    '/admin/dashboard': '数据看板',
    '/admin/songs': '歌曲管理',
    '/admin/singers': '歌手管理',
    '/admin/playlists': '歌单管理',
    '/admin/comments': '评论管理',
    '/admin/community': '社区管理',
    '/admin/announcements': '公告管理',
    '/admin/users': '用户管理',
    '/admin/operation-logs': '操作日志',
    '/admin/settings': '系统设置'
  }
  return map[route.path] || ''
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: #0f0f1a;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: var(--gradient-primary);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.1);
  border-radius: 2px;
}

.menu-group {
  margin-bottom: 8px;
}

.menu-group-title {
  padding: 8px 24px 4px;
  font-size: 11px;
  font-weight: 600;
  color: rgba(255,255,255,0.3);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  color: rgba(255,255,255,0.6);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
  cursor: pointer;
  position: relative;
}

.menu-item:hover {
  color: rgba(255,255,255,0.9);
  background: rgba(255,255,255,0.04);
}

.menu-item.active {
  color: #fff;
  background: linear-gradient(90deg, rgba(102,126,234,0.2) 0%, transparent 100%);
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: linear-gradient(180deg, #667eea, #764ba2);
  border-radius: 0 3px 3px 0;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  color: rgba(255,255,255,0.5);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
  font-size: 13px;
}

.back-link:hover {
  color: rgba(255,255,255,0.9);
  background: rgba(255,255,255,0.06);
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  height: 56px;
  background: rgba(26, 26, 46, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255,255,255,0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-item {
  color: rgba(255,255,255,0.4);
  cursor: pointer;
}

.breadcrumb-item:hover {
  color: rgba(255,255,255,0.7);
}

.breadcrumb-sep {
  color: rgba(255,255,255,0.2);
}

.breadcrumb-current {
  color: rgba(255,255,255,0.8);
  font-weight: 500;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.admin-name {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #0f0f1a;
}

.content-area::-webkit-scrollbar {
  width: 6px;
}

.content-area::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.1);
  border-radius: 3px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
