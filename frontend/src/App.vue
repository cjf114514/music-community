<template>
  <div class="app-container" :class="{ 'neon-theme': true }">
    <DynamicBackground />
    <!--
      showLayout 控制是否显示完整布局
      guest 页面 (登录/注册/忘记密码) → 不显示侧边栏和播放器,只渲染登录表单
      其他页面 → 显示完整布局 (侧边栏 + 头部 + 主内容 + 播放器 + 播放列表抽屉)
    -->
    <template v-if="showLayout">
      <!-- 移动端顶栏：只在窄屏出现，用来打开侧边栏抽屉 -->
      <div class="mobile-topbar">
        <button class="m-menu-btn" aria-label="打开菜单" @click="uiStore.toggleSidebar()">
          <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22"><path d="M3 6h18v2H3V6zm0 5h18v2H3v-2zm0 5h18v2H3v-2z"/></svg>
        </button>
        <span class="m-title">NEON MUSIC</span>
      </div>

      <Sidebar />
      <!-- 抽屉打开时的遮罩，点击关闭 -->
      <div v-if="uiStore.sidebarOpen" class="mobile-overlay" @click="uiStore.closeSidebar()"></div>

      <div class="app-main">
        <Header />
        <main class="main-content">
          <!-- 路由出口 — 当前 URL 对应的页面组件在这里渲染 -->
          <router-view />
        </main>
      </div>
      <!-- Player 和 PlayListDrawer 是全局持久的 — 无论切到哪个页面都存在 -->
      <Player />
      <PlayListDrawer />
    </template>
    <template v-else>
      <router-view />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import Player from './components/Player.vue'
import PlayListDrawer from './components/PlayListDrawer.vue'
import DynamicBackground from './components/DynamicBackground.vue'
import { useThemeStore } from '@/stores/theme'
import { useMusicStore } from '@/stores/music'
import { useUiStore } from '@/stores/ui'

const route = useRoute()

/**
 * 是否显示完整布局
 * guest: true 的路由 (登录/注册/忘记密码) → 不显示侧边栏和播放器
 * 原因: 这些页面是独立页面,不需要网站框架
 */
const showLayout = computed(() => !route.meta.guest)

const themeStore = useThemeStore()
const musicStore = useMusicStore()
const uiStore = useUiStore()

/**
 * 全局键盘快捷键处理
 *
 * 为什么注册在 App.vue 而不是 Player.vue?
 *   App.vue 在应用生命周期内始终挂载 → 快捷键始终生效
 *   Player.vue 切换歌曲时也不会重新挂载
 *
 * addEventListener 第三个参数 = true:
 *   在"捕获阶段"监听 → 比其他元素更早拿到事件 → e.stopPropagation() 阻止传递
 *   这是解决 Element Plus 组件 (如 el-slider) 消耗方向键事件的关键
 *
 * 输入框防护:
 *   用户在 input/textarea/select 中打字 → 不触发快捷键
 *   弹窗/抽屉打开时 → 不触发快捷键
 */
function handleKeydown(e) {
  // 如果焦点在输入框中 → 忽略
  const tag = e.target.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return
  if (e.target.isContentEditable) return

  // 如果有可见的弹窗或抽屉 → 忽略 (避免在对话框里误触)
  const visibleDialog = document.querySelector('.el-dialog:not([style*="display: none"])')
  const visibleDrawer = document.querySelector('.el-drawer:not([style*="display: none"])')
  if (visibleDialog || visibleDrawer) return

  switch (e.code) {
    case 'Space':
      e.preventDefault()
      e.stopPropagation()
      musicStore.togglePlay()
      break
    case 'ArrowLeft':
      e.preventDefault()
      e.stopPropagation()
      if (e.ctrlKey || e.metaKey) {
        musicStore.playPrev()  // Ctrl+左 = 上一首
      } else {
        const audioEl = document.querySelector('audio')
        if (audioEl) audioEl.currentTime = Math.max(0, audioEl.currentTime - 5) // 快退 5 秒
      }
      break
    case 'ArrowRight':
      e.preventDefault()
      e.stopPropagation()
      if (e.ctrlKey || e.metaKey) {
        musicStore.playNext()  // Ctrl+右 = 下一首
      } else {
        const audioEl = document.querySelector('audio')
        if (audioEl) audioEl.currentTime = Math.min(audioEl.duration || 0, audioEl.currentTime + 5) // 快进 5 秒
      }
      break
    case 'ArrowUp':
      e.preventDefault()
      e.stopPropagation()
      musicStore.setVolume(Math.min(100, musicStore.volume + 5))  // 音量 +5
      {
        const audioEl = document.querySelector('audio')
        if (audioEl) audioEl.volume = musicStore.volume / 100
      }
      break
    case 'ArrowDown':
      e.preventDefault()
      e.stopPropagation()
      musicStore.setVolume(Math.max(0, musicStore.volume - 5))  // 音量 -5
      {
        const audioEl = document.querySelector('audio')
        if (audioEl) audioEl.volume = musicStore.volume / 100
      }
      break
    case 'KeyM':
      if (e.ctrlKey || e.metaKey) return
      e.preventDefault()
      e.stopPropagation()
      {
        const audioEl = document.querySelector('audio')
        if (audioEl) audioEl.muted = !audioEl.muted  // 切换静音
      }
      break
  }
}

onMounted(() => {
  themeStore.initTheme()  // 初始化主题 (读取 localStorage 中的主题设置)
  // 注册键盘快捷键,第三个参数 true = 捕获阶段
  document.addEventListener('keydown', handleKeydown, true)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown, true)
})
</script>

<style>
/* ==================== CSS 变量系统 ====================
 * 通过 :root 定义暗色主题变量,.light 覆盖为亮色主题
 * 变量命名: --用途 (如 --bg-primary = 主背景色, --text-primary = 主文字色)
 * 好处: 切换主题只需要加/删 .light 类,不用改每个组件
 * 用法: background: var(--bg-primary); (var() = 读取 CSS 变量)
 * ==================================================== */

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 暗色主题 (默认) */
:root {
  --primary-color: #818cf8;
  --primary-hover: #a5b4fc;
  --primary-light: rgba(129, 140, 248, 0.12);
  --primary-deep: #6366f1;
  --accent-cyan: #22d3ee;
  --accent-pink: #f472b6;
  --accent-amber: #fbbf24;
  --bg-primary: #07070e;
  --bg-secondary: #0f0f1a;
  --bg-elevated: #161625;
  --bg-card: #12121e;
  --sidebar-bg: rgba(12, 12, 22, 0.85);
  --text-primary: #e2e8f0;
  --text-secondary: #94a3b8;
  --text-tertiary: #64748b;
  --border-color: rgba(99, 102, 241, 0.12);
  --border-glow: rgba(99, 102, 241, 0.25);
  --card-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
  --card-hover-shadow: 0 8px 40px rgba(99, 102, 241, 0.15);
  --nav-hover-bg: rgba(99, 102, 241, 0.08);
  --nav-active-bg: rgba(99, 102, 241, 0.15);
  --scrollbar-thumb: rgba(99, 102, 241, 0.3);
  --gradient-primary: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a78bfa 100%);
  --gradient-neon: linear-gradient(135deg, #6366f1 0%, #22d3ee 100%);
  --gradient-warm: linear-gradient(135deg, #f472b6 0%, #8b5cf6 100%);
  --gradient-cool: linear-gradient(135deg, #22d3ee 0%, #6366f1 100%);
  --gradient-sunset: linear-gradient(135deg, #fbbf24 0%, #f472b6 100%);
  --gradient-ocean: linear-gradient(135deg, #6366f1 0%, #22d3ee 100%);
  --gradient-forest: linear-gradient(135deg, #34d399 0%, #22d3ee 100%);
  --neon-glow: 0 0 20px rgba(99, 102, 241, 0.3), 0 0 60px rgba(99, 102, 241, 0.1);
  --neon-glow-sm: 0 0 10px rgba(99, 102, 241, 0.2);
  --neon-glow-cyan: 0 0 20px rgba(34, 211, 238, 0.3), 0 0 60px rgba(34, 211, 238, 0.1);
  --neon-glow-pink: 0 0 20px rgba(244, 114, 182, 0.3), 0 0 60px rgba(244, 114, 182, 0.1);
  --glass-bg: rgba(15, 15, 26, 0.7);
  --glass-border: rgba(99, 102, 241, 0.1);
}

/* 亮色主题 — 覆盖暗色变量 */
.light {
  --primary-color: #6366f1;
  --primary-hover: #4f46e5;
  --primary-light: rgba(99, 102, 241, 0.08);
  --primary-deep: #4f46e5;
  --accent-cyan: #06b6d4;
  --accent-pink: #ec4899;
  --accent-amber: #f59e0b;
  --bg-primary: #f8fafc;
  --bg-secondary: #ffffff;
  --bg-elevated: #f1f5f9;
  --bg-card: #ffffff;
  --sidebar-bg: rgba(255, 255, 255, 0.9);
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-tertiary: #94a3b8;
  --border-color: rgba(99, 102, 241, 0.1);
  --border-glow: rgba(99, 102, 241, 0.2);
  --card-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  --card-hover-shadow: 0 8px 30px rgba(99, 102, 241, 0.1);
  --nav-hover-bg: rgba(99, 102, 241, 0.06);
  --nav-active-bg: rgba(99, 102, 241, 0.1);
  --scrollbar-thumb: rgba(99, 102, 241, 0.2);
  --neon-glow: 0 2px 12px rgba(99, 102, 241, 0.1);
  --neon-glow-sm: 0 1px 6px rgba(99, 102, 241, 0.08);
  --neon-glow-cyan: 0 2px 12px rgba(6, 182, 212, 0.1);
  --neon-glow-pink: 0 2px 12px rgba(236, 72, 153, 0.1);
  --glass-bg: rgba(255, 255, 255, 0.8);
  --glass-border: rgba(99, 102, 241, 0.08);
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: background 0.4s ease, color 0.3s ease;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.app-container {
  min-height: 100vh;
  padding-bottom: 80px;     /* 为底部固定播放器留空间 (Player 高 72px + 进度条 3px) */
  position: relative;
  z-index: 1;
}

.app-main {
  margin-left: 240px;       /* 左侧导航栏宽度 */
  min-height: 100vh;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 32px;
}

/* ==================== 移动端（≤768px）====================
 * 桌面用「固定侧边栏 + 主区左移 240px」的布局；
 * 窄屏放不下，改成：
 *   · 侧边栏变抽屉，默认藏在屏幕外（见 Sidebar.vue）
 *   · 顶部出现一个 52px 的标题栏，左侧是菜单按钮
 *   · 主区不再左移，内容和播放器留出安全区内边距
 * ==================================================== */
.mobile-topbar { display: none; }
.mobile-overlay { display: none; }

@media (max-width: 768px) {
  .mobile-topbar {
    display: flex;
    align-items: center;
    gap: 8px;
    position: fixed;
    top: 0; left: 0; right: 0;
    height: calc(52px + env(safe-area-inset-top, 0px));
    padding: env(safe-area-inset-top, 0px) 10px 0;
    z-index: 130;
    background: rgba(15, 15, 26, 0.94);
    backdrop-filter: blur(14px);
    -webkit-backdrop-filter: blur(14px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  }
  .m-menu-btn {
    background: none; border: none; padding: 6px 8px;
    color: rgba(255, 255, 255, 0.82); cursor: pointer; display: flex; align-items: center;
  }
  .m-title {
    font-size: 14px; font-weight: 600; letter-spacing: 1.5px;
    color: rgba(255, 255, 255, 0.9);
  }

  .mobile-overlay {
    display: block;
    position: fixed; inset: 0;
    background: rgba(0, 0, 0, 0.55);
    z-index: 115;
  }

  /* 主区铺满，顶部让出标题栏高度 */
  .app-main {
    margin-left: 0 !important;
    padding-top: calc(52px + env(safe-area-inset-top, 0px));
  }

  .main-content {
    padding: 14px 12px calc(84px + env(safe-area-inset-bottom, 0px));
  }

  /* 列表类表格在窄屏横向滚动，不要挤压变形 */
  .main-content table { min-width: 520px; }

  /* 窄屏下滚动条隐藏，避免占位 */
  ::-webkit-scrollbar { width: 0; height: 0; }
}

/* 自定义滚动条样式 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--scrollbar-thumb); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--primary-color); }

/* ==================== Element Plus 全局样式覆盖 ====================
 * 用 !important 覆盖 Element Plus 默认样式,使 UI 组件和主题一致
 * 这些样式对所有使用 Element Plus 组件的页面生效
 * ==================================================== */

.el-button--primary {
  background: var(--gradient-primary) !important; border: none !important; box-shadow: var(--neon-glow-sm);
}
.el-button--primary:hover { opacity: 0.9; box-shadow: var(--neon-glow); }
.el-input__wrapper {
  background-color: var(--bg-elevated) !important; box-shadow: 0 0 0 1px var(--border-color) inset !important; border-radius: 10px !important;
}
.el-input__wrapper:hover { box-shadow: 0 0 0 1px var(--border-glow) inset !important; }
.el-input__wrapper.is-focus { box-shadow: 0 0 0 1px var(--primary-color) inset, var(--neon-glow-sm) !important; }
.el-input__inner { color: var(--text-primary) !important; }
.el-input__inner::placeholder { color: var(--text-tertiary) !important; }
.el-card { background-color: var(--bg-card) !important; border: 1px solid var(--border-color) !important; border-radius: 16px !important; box-shadow: var(--card-shadow) !important; }
.el-card__body { color: var(--text-primary) !important; }
.el-dialog { background-color: var(--bg-secondary) !important; border: 1px solid var(--border-color) !important; border-radius: 16px !important; box-shadow: var(--neon-glow) !important; }
.el-dialog__title, .el-dialog__body { color: var(--text-primary) !important; }
.el-dialog__header { border-bottom: 1px solid var(--border-color); }
.el-form-item__label { color: var(--text-primary) !important; }
.el-button {
  --el-button-bg-color: var(--bg-elevated); --el-button-border-color: var(--border-color); --el-button-text-color: var(--text-primary);
  --el-button-hover-bg-color: var(--nav-hover-bg); --el-button-hover-border-color: var(--border-glow); --el-button-hover-text-color: var(--primary-color);
}
.el-dropdown-menu { background-color: var(--bg-secondary) !important; border: 1px solid var(--border-color) !important; border-radius: 12px !important; box-shadow: var(--card-hover-shadow) !important; padding: 6px !important; }
.el-dropdown-menu__item { color: var(--text-primary) !important; border-radius: 8px !important; }
.el-dropdown-menu__item:hover { background-color: var(--nav-hover-bg) !important; color: var(--primary-color) !important; }
.el-slider__runway { background-color: var(--bg-elevated) !important; }
.el-slider__bar { background: var(--gradient-primary) !important; }
.el-slider__button { border-color: var(--primary-color) !important; background-color: var(--primary-color) !important; box-shadow: var(--neon-glow-sm) !important; }
.el-drawer { background-color: var(--bg-secondary) !important; }
.el-drawer__title, .el-drawer__body { color: var(--text-primary) !important; }
.el-tabs__item { color: var(--text-secondary) !important; }
.el-tabs__item:hover, .el-tabs__item.is-active { color: var(--primary-color) !important; }
.el-tabs__active-bar { background: var(--gradient-primary) !important; }
.el-table { background-color: transparent !important; color: var(--text-primary) !important; }
.el-table th.el-table__cell { background-color: var(--bg-elevated) !important; color: var(--text-secondary) !important; border-color: var(--border-color) !important; }
.el-table td.el-table__cell { border-color: var(--border-color) !important; color: var(--text-primary) !important; background-color: transparent !important; }
.el-pager li { background-color: var(--bg-elevated) !important; color: var(--text-primary) !important; border-radius: 8px !important; }
.el-pager li:hover { color: var(--primary-color) !important; }
.el-pager li.is-active { background: var(--gradient-primary) !important; color: #fff !important; }
</style>
