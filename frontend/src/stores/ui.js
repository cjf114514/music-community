import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 界面状态（目前只用来管移动端侧边栏抽屉）
 *
 * 为什么单独建一个 store？
 *   侧边栏在 Sidebar.vue 里，开关按钮在 App.vue 的移动端顶栏里，
 *   两者是兄弟组件，靠 props/emit 传会很绕，放 store 最直接。
 */
export const useUiStore = defineStore('ui', () => {
  const sidebarOpen = ref(false)

  const toggleSidebar = () => { sidebarOpen.value = !sidebarOpen.value }
  const closeSidebar = () => { sidebarOpen.value = false }

  return { sidebarOpen, toggleSidebar, closeSidebar }
})
