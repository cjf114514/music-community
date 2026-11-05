<template>
  <aside class="sidebar">
    <div class="sidebar-logo" @click="$router.push('/')">
      <div class="logo-icon">
        <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
      </div>
      <span class="logo-text">NEON<span class="logo-accent">MUSIC</span></span>
    </div>

    <div class="sidebar-section">
      <div class="section-title">发现</div>
      <nav class="nav-menu">
        <div
          v-for="item in mainNav"
          :key="item.path"
          :class="['nav-item', { active: isActive(item.path) }]"
          @click="$router.push(item.path)"
        >
          <div class="nav-icon">
            <svg v-if="item.icon === 'Compass'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-5.5-2.5l7.51-3.49L17.5 6.5 9.99 9.99 6.5 17.5zm5.5-6.6c.61 0 1.1.49 1.1 1.1s-.49 1.1-1.1 1.1-1.1-.49-1.1-1.1.49-1.1 1.1-1.1z"/></svg>
            <svg v-else-if="item.icon === 'Collection'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H8V4h12v12zm-8-1h2V9h-2v2H9v2h3z"/></svg>
            <svg v-else-if="item.icon === 'TrendCharts'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z"/></svg>
            <svg v-else-if="item.icon === 'ChatDotSquare'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zM7 9h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z"/></svg>
          </div>
          <span>{{ item.label }}</span>
        </div>
      </nav>
    </div>

    <div v-if="userStore.isLoggedIn()" class="sidebar-section">
      <div class="section-title">我的音乐</div>
      <nav class="nav-menu">
        <div
          v-for="item in myNav"
          :key="item.path"
          :class="['nav-item', { active: isActive(item.path) }]"
          @click="$router.push(item.path)"
        >
          <div class="nav-icon">
            <svg v-if="item.icon === 'User'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <svg v-else-if="item.icon === 'StarFilled'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
            <svg v-else-if="item.icon === 'Clock'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/></svg>
            <svg v-else-if="item.icon === 'FolderOpened'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M20 6h-8l-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z"/></svg>
            <svg v-else-if="item.icon === 'MagicStick'" viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M7.5 5.6L10 7 8.6 4.5 10 2 7.5 3.4 5 2l1.4 2.5L5 7zm12 9.8L17 14l1.4 2.5L17 19l2.5-1.4L22 19l-1.4-2.5L22 14zM22 2l-2.5 1.4L17 2l1.4 2.5L17 7l2.5-1.4L22 7l-1.4-2.5zm-7.63 5.29a.996.996 0 0 0-1.41 0L1.29 18.96a.996.996 0 0 0 0 1.41l2.34 2.34c.39.39 1.02.39 1.41 0L16.7 11.05a.996.996 0 0 0 0-1.41l-2.33-2.35zm-1.03 5.49l-2.12-2.12 2.44-2.44 2.12 2.12-2.44 2.44z"/></svg>
          </div>
          <span>{{ item.label }}</span>
        </div>
      </nav>
    </div>

    <div class="sidebar-glow"></div>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const mainNav = [
  { path: '/', label: '发现音乐', icon: 'Compass' },
  { path: '/playlists', label: '歌单广场', icon: 'Collection' },
  { path: '/ranking', label: '排行榜', icon: 'TrendCharts' },
  { path: '/community', label: '音乐社区', icon: 'ChatDotSquare' },
]

const myNav = [
  { path: '/profile', label: '个人中心', icon: 'User' },
  { path: '/my-favorites', label: '我喜欢的音乐', icon: 'StarFilled' },
  { path: '/recent-plays', label: '最近播放', icon: 'Clock' },
  { path: '/smart-playlist', label: '智能歌单', icon: 'MagicStick' },
  { path: '/my-playlists', label: '我的歌单', icon: 'FolderOpened' },
]

const isActive = (path) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<style scoped>
.sidebar {
  width: 240px;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background: var(--sidebar-bg);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid var(--border-color);
  overflow-y: auto;
  z-index: 100;
  padding: 20px 0;
}

.sidebar-glow {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: linear-gradient(to top, rgba(99, 102, 241, 0.03), transparent);
  pointer-events: none;
}

.sidebar::-webkit-scrollbar {
  width: 4px;
}

.sidebar::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 2px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  margin-bottom: 28px;
  cursor: pointer;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: var(--neon-glow-sm);
}

.logo-text {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 2px;
  color: var(--text-primary);
}

.logo-accent {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-section {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  margin-bottom: 8px;
  font-size: 11px;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 2px;
  font-weight: 600;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  position: relative;
  border-radius: 10px;
}

.nav-item:hover {
  background: var(--nav-hover-bg);
  color: var(--text-primary);
}

.nav-item.active {
  color: var(--primary-color);
  background: var(--nav-active-bg);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  background: var(--gradient-primary);
  border-radius: 0 3px 3px 0;
  box-shadow: var(--neon-glow-sm);
}

.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  opacity: 0.7;
}

.nav-item.active .nav-icon {
  opacity: 1;
}
</style>
