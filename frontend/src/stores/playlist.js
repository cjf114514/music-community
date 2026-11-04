import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserPlaylists } from '@/api/song'
import { useUserStore } from './user'

export const usePlaylistStore = defineStore('playlist', () => {
  const myPlaylists = ref([])

  const fetchPlaylists = async () => {
    const userStore = useUserStore()
    if (!userStore.userInfo?.userId) return
    try {
      const res = await getUserPlaylists(userStore.userInfo.userId)
      myPlaylists.value = res.data || []
    } catch (e) {}
  }

  const removePlaylist = (id) => {
    myPlaylists.value = myPlaylists.value.filter(pl => pl.id !== id)
  }

  const addPlaylist = (playlist) => {
    myPlaylists.value.unshift(playlist)
  }

  const updatePlaylistInList = (id, data) => {
    const index = myPlaylists.value.findIndex(pl => pl.id === id)
    if (index !== -1) {
      myPlaylists.value[index] = { ...myPlaylists.value[index], ...data }
    }
  }

  return { myPlaylists, fetchPlaylists, removePlaylist, addPlaylist, updatePlaylistInList }
})
