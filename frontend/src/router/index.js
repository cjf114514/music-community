import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置 — 决定 URL 和页面之间的映射关系
 *
 * ========== 核心概念 ==========
 * 路由 = URL 路径 → 对应显示的 Vue 组件
 * 例如: 用户访问 /login → 显示 Login.vue 页面
 *
 * ========== 路由懒加载 ==========
 * component: () => import('@/views/Home.vue')
 * 这不是直接导入,而是在路由被访问时才加载
 * 好处: 首屏只加载当前页面需要的代码,而不是全部 28 个页面
 * 结果: 首次加载更快,打包后的 js 文件会自动拆分成多个 chunk
 *
 * ========== meta 元信息 ==========
 * guest: true        → 仅游客可访问 (如 /login,已登录用户会跳回首页)
 * requiresAuth: true → 需要登录 (未登录会跳 /login)
 * requiresAdmin: true → 需要管理员 (非管理员会跳首页)
 *
 * ========== 路由守卫 (beforeEach) ==========
 * 每次路由切换前都会执行,相当于后端的"拦截器"
 * to:   要去的路由信息
 * from: 来自哪个路由
 * next: 放行函数: next()=通过, next('/login')=重定向
 *
 * ========== createWebHistory() ==========
 * 使用 HTML5 History 模式 (URL 中没有 # 号)
 * 区别:
 *   createWebHistory() → /login (干净,需要服务器配置 fallback)
 *   createWebHashHistory() → /#/login (丑陋,但不需要服务器配置)
 */

const routes = [
  // ==================== 游客页面 (已登录则跳首页) ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPassword.vue'),
    meta: { guest: true }
  },

  // ==================== 公开页面 (无需登录) ====================
  { path: '/', name: 'Home', component: () => import('@/views/Home.vue') },
  { path: '/search', name: 'Search', component: () => import('@/views/Search.vue') },
  { path: '/playlists', name: 'PlaylistSquare', component: () => import('@/views/PlaylistSquare.vue') },
  { path: '/playlist/:id', name: 'PlaylistDetail', component: () => import('@/views/PlaylistDetail.vue') },
  // :id = 动态路由参数, /playlist/5 → id=5
  { path: '/song/:id', name: 'SongDetail', component: () => import('@/views/SongDetail.vue') },
  { path: '/ranking', name: 'Ranking', component: () => import('@/views/Ranking.vue') },
  { path: '/smart-playlist', name: 'SmartPlaylist', component: () => import('@/views/SmartPlaylist.vue') },
  { path: '/singer/:name', name: 'SingerDetail', component: () => import('@/views/SingerDetail.vue') },
  { path: '/album/:name', name: 'AlbumDetail', component: () => import('@/views/AlbumDetail.vue') },
  { path: '/community', name: 'Community', component: () => import('@/views/Community.vue') },
  { path: '/community/post/:id', name: 'PostDetail', component: () => import('@/views/PostDetail.vue') },
  { path: '/shared-playlist/:token', name: 'SharedPlaylist', component: () => import('@/views/SharedPlaylist.vue') },
  { path: '/shared-song/:token', name: 'SharedSong', component: () => import('@/views/SharedSong.vue') },

  // ==================== 需登录页面 ====================
  { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/edit-profile', name: 'EditProfile', component: () => import('@/views/EditProfile.vue'), meta: { requiresAuth: true } },
  { path: '/account-settings', name: 'AccountSettings', component: () => import('@/views/AccountSettings.vue'), meta: { requiresAuth: true } },
  { path: '/song-manage', name: 'SongManage', component: () => import('@/views/SongManage.vue'), meta: { requiresAuth: true } },
  { path: '/my-playlists', name: 'MyPlaylists', component: () => import('@/views/MyPlaylists.vue'), meta: { requiresAuth: true } },
  { path: '/my-favorites', name: 'MyFavorites', component: () => import('@/views/MyFavorites.vue'), meta: { requiresAuth: true } },
  { path: '/recent-plays', name: 'RecentPlays', component: () => import('@/views/RecentPlays.vue'), meta: { requiresAuth: true } },
  { path: '/daily-recommend', name: 'DailyRecommend', component: () => import('@/views/DailyRecommend.vue'), meta: { requiresAuth: true } },
  { path: '/user/:id', name: 'UserProfile', component: () => import('@/views/UserProfile.vue'), meta: { requiresAuth: true } },
  { path: '/messages', name: 'Messages', component: () => import('@/views/Messages.vue'), meta: { requiresAuth: true } },

  // ==================== 管理员页面 (嵌套路由) ====================
  // children: 子路由 — 在 AdminLayout.vue 的 <router-view /> 中渲染
  // /admin 自动重定向到 /admin/dashboard
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'users', name: 'AdminUserManage', component: () => import('@/views/admin/UserManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'songs', name: 'AdminSongManage', component: () => import('@/views/admin/SongManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'batch-import', name: 'AdminBatchImport', component: () => import('@/views/admin/BatchImport.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'library', name: 'AdminLibrary', component: () => import('@/views/admin/Library.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'playlists', name: 'AdminPlaylistManage', component: () => import('@/views/admin/PlaylistManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'comments', name: 'AdminCommentManage', component: () => import('@/views/admin/CommentManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'community', name: 'AdminCommunityManage', component: () => import('@/views/admin/CommunityManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'announcements', name: 'AdminAnnouncementManage', component: () => import('@/views/admin/AnnouncementManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'operation-logs', name: 'AdminOperationLog', component: () => import('@/views/admin/OperationLog.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'settings', name: 'AdminSystemSettings', component: () => import('@/views/admin/SystemSettings.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(), // HTML5 History 模式,URL 干净无 #
  routes
})

/**
 * 全局前置路由守卫
 *
 * 每次路由切换前都会经过这里,检查权限
 * 相当于前端的"拦截器" — 和后端 JwtInterceptor 配合使用:
 *   后端负责接口级权限 (能不能调用这个 API)
 *   前端负责页面级权限 (能不能看到这个页面)
 *
 * 逻辑:
 *   1. 页面要登录 + 你没有 token → 跳到登录页
 *   2. 页面仅游客 + 你已登录 → 跳到首页 (已登录不需要看登录页)
 *   3. 页面要管理员 + 你不是 ADMIN → 跳到首页
 *   4. 其他情况 → 正常通过
 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  if (to.meta.requiresAuth && !token) {
    // 需要登录但没 token → 跳登录页
    next('/login')
  } else if (to.meta.guest && token) {
    // 游客页面但已登录 → 跳首页 (已登录了还看什么登录页)
    next('/')
  } else if (to.meta.requiresAdmin && (!user || user.role !== 'ADMIN')) {
    // 需要管理员但不是 ADMIN → 跳首页
    next('/')
  } else {
    next()  // 放行
  }
})

export default router
