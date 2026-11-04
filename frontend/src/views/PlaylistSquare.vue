<template>
  <div class="playlist-square">
    <div class="page-header">
      <h2 class="page-title">歌单广场</h2>
      <el-button v-if="userStore.isLoggedIn()" type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> 创建歌单
      </el-button>
    </div>

    <div class="category-tabs" v-if="categories.length">
      <span
        :class="['category-tab', { active: activeCategory === '' }]"
        @click="filterByCategory('')"
      >全部</span>
      <span
        v-for="cat in categories"
        :key="cat"
        :class="['category-tab', { active: activeCategory === cat }]"
        @click="filterByCategory(cat)"
      >{{ cat }}</span>
    </div>

    <div class="playlist-grid">
      <div v-for="pl in playlists" :key="pl.id" class="playlist-card" @click="goToDetail(pl.id)">
        <div class="playlist-cover">
          <el-image v-if="pl.coverUrl" :src="pl.coverUrl" fit="cover" />
          <div v-else class="cover-placeholder">
            <el-icon :size="40"><Music /></el-icon>
          </div>
          <div class="song-count">
            <el-icon><Headset /></el-icon> {{ pl.songCount || 0 }}首
          </div>
        </div>
        <div class="playlist-info">
          <h3 class="playlist-name">{{ pl.name }}</h3>
          <p class="playlist-desc">{{ pl.description || '暂无描述' }}</p>
        </div>
      </div>
    </div>

    <el-dialog v-model="showCreateDialog" title="创建歌单" width="500px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="歌单封面">
          <el-upload
            ref="coverUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".jpg,.jpeg,.png,.gif,.webp"
            :on-change="handleCoverChange"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="歌单名称" required>
          <el-input v-model="createForm.name" placeholder="请输入歌单名称" />
        </el-form-item>
        <el-form-item label="歌单描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="简单描述一下你的歌单" />
        </el-form-item>
        <el-form-item label="公开设置">
          <el-radio-group v-model="createForm.isPublic">
            <el-radio :value="1">公开</el-radio>
            <el-radio :value="0">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="歌单分类">
          <el-input v-model="createForm.category" placeholder="如：华语、流行、摇滚、电子、民谣..." maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPublicPlaylists, getPlaylistCategories, createPlaylist, uploadCover } from '@/api/song'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const playlists = ref([])
const categories = ref([])
const activeCategory = ref('')
const showCreateDialog = ref(false)
const creating = ref(false)
const coverFile = ref(null)

const createForm = ref({
  name: '',
  description: '',
  isPublic: 1,
  category: '',
  userId: null
})

const loadPlaylists = async () => {
  const res = await getPublicPlaylists(activeCategory.value || undefined)
  playlists.value = res.data || []
}

const loadCategories = async () => {
  try {
    const res = await getPlaylistCategories()
    categories.value = res.data || []
  } catch (e) {}
}

const filterByCategory = async (category) => {
  activeCategory.value = category
  await loadPlaylists()
}

onMounted(async () => {
  await loadCategories()
  await loadPlaylists()
})

const goToDetail = (id) => {
  router.push(`/playlist/${id}`)
}

const handleCoverChange = (file) => {
  coverFile.value = file.raw
}

const handleCreate = async () => {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入歌单名称')
    return
  }
  if (!userStore.userInfo?.userId) {
    ElMessage.error('请先登录')
    return
  }
  creating.value = true
  try {
    let coverUrl = null
    if (coverFile.value) {
      const coverRes = await uploadCover(coverFile.value)
      coverUrl = coverRes.data
    }
    const payload = {
      name: createForm.value.name,
      description: createForm.value.description,
      isPublic: createForm.value.isPublic,
      category: createForm.value.category || undefined,
      userId: userStore.userInfo.userId,
      coverUrl
    }
    await createPlaylist(payload)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    coverFile.value = null
    createForm.value = { name: '', description: '', isPublic: 1, category: '', userId: null }
    await loadPlaylists()
    await loadCategories()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.playlist-square {
  padding: 20px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  color: var(--text-primary);
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-elevated);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--border-color);
}

.category-tab:hover {
  color: var(--text-primary);
  border-color: var(--primary-color);
}

.category-tab.active {
  color: #fff;
  background: var(--gradient-primary);
  border-color: transparent;
}

.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.playlist-card {
  background: var(--bg-secondary);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.playlist-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.playlist-cover {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: var(--gradient-primary);
}

.playlist-cover .el-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
}

.song-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.playlist-info {
  padding: 12px;
}

.playlist-name {
  font-size: 14px;
  color: var(--text-primary);
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
