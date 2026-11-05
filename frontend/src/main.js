import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

/**
 * Vue 应用入口文件 — 整个前端项目的启动点
 *
 * ========== 这个文件做了什么？==========
 * 1. 创建 Vue 应用实例 (createApp)
 * 2. 注册 Pinia 状态管理 (类似后端的 Service 层,管理全局数据)
 * 3. 注册 Vue Router 路由 (决定 URL 对应哪个页面)
 * 4. 注册 Element Plus UI 组件库 (提供按钮、表单、对话框等)
 * 5. 全局注册所有 Element Plus 图标 (方便在任何组件直接用 <el-icon>)
 * 6. 挂载到 index.html 中的 #app 元素
 *
 * ========== .use() 链式调用 ==========
 * app.use(插件) 是 Vue 的插件注册机制
 * 注册后,插件提供的功能在全局可用
 * 例如: app.use(pinia) 后,任何组件都可以 import { useMusicStore } from '@/stores/music'
 *
 * ========== 关键概念 ==========
 * createPinia(): 创建一个 Pinia 实例
 *   Pinia 是 Vue 3 官方推荐的状态管理库 (替代旧版 Vuex)
 *   用于在组件间共享数据,比如:
 *     - 当前播放的歌曲 → music store
 *     - 用户登录状态 → user store
 *     - 主题设置 → theme store
 *
 * #app: index.html 里的挂载点 <div id="app"></div>
 *   整个 Vue 应用渲染在这个 div 里面
 */

const app = createApp(App)   // 创建 Vue 应用,App.vue 是根组件
const pinia = createPinia()   // 创建 Pinia 状态管理实例

// 逐个注册所有 Element Plus 图标组件
// 这样在任何 .vue 文件中可以直接用 <el-icon><Music /></el-icon> 等形式
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)    // 注册状态管理
app.use(router)   // 注册路由
app.use(ElementPlus) // 注册 UI 组件库
app.mount('#app') // 挂载到页面 — 从这一刻起,Vue 开始接管 DOM

/**
 * 注册 Service Worker —— 让网站支持"添加到主屏幕"和离线打开
 *
 * 为什么要放在 load 之后？
 *   避免和首屏资源加载抢带宽，先让页面出来再后台装 SW。
 * 失败不报错：SW 只是增强能力，注册不上网站照样能用。
 */
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      /* 开发环境或非 HTTPS 下可能注册失败，静默忽略 */
    })
  })
}
