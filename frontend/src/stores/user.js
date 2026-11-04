import { defineStore } from 'pinia'
import { ref } from 'vue'

function parseUser() {
  try {
    const user = localStorage.getItem('user')
    return user ? JSON.parse(user) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(parseUser())

  function setToken(t) {
    token.value = t
    if (t) {
      localStorage.setItem('token', t)
    } else {
      localStorage.removeItem('token')
    }
  }

  function setUserInfo(info) {
    userInfo.value = info
    if (info) {
      localStorage.setItem('user', JSON.stringify(info))
    } else {
      localStorage.removeItem('user')
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const isLoggedIn = () => !!token.value

  return { token, userInfo, setToken, setUserInfo, logout, isLoggedIn }
})
