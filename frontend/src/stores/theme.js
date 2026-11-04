import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem('theme') !== 'light')

  function toggleTheme() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    document.documentElement.classList.toggle('light', !isDark.value)
  }

  function initTheme() {
    document.documentElement.classList.toggle('light', !isDark.value)
  }

  return { isDark, toggleTheme, initTheme }
})
