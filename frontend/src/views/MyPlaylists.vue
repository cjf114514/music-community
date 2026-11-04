<template>
  <div class="my-playlists">
    <div class="page-header">
      <h2 class="page-title">我的歌单</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> 创建歌单
      </el-button>
    </div>

    <div class="playlist-grid" v-loading="loading">
      <div v-for="pl in playlists" :key="pl.id" class="playlist-card">
        <div class="playlist-cover" @click="goToDetail(pl.id)">
          <el-image v-if="pl.coverUrl" :src="pl.coverUrl" fit="cover" />
          <div v-else class="cover-placeholder">
            <el-icon :size="40"><Music /></el-icon>
          </div>
          <div class="song-count">
            <el-icon><Headset /></el-icon> {{ pl.songCount || 0 }}首
          </div>
        </div>
        <div class="playlist-info">
          <h3 class="playlist-name" @click="goToDetail(pl.id)">{{ pl.name }}</h3>
          <p class="playlist-desc">{{ pl.description || '暂无描述' }}</p>
          <div class="playlist-actions">
            <div class="playlist-action-btn" @click="handleEdit(pl)">
              <el-icon><Edit /></el-icon> 编辑
            </div>
            <div class="playlist-action-btn delete" @click="handleDelete(pl.id)">
              <el-icon><Delete /></el-icon> 删除
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !playlists.length" description="暂无歌单，创建一个吧" />

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

    <el-dialog v-model="showEditDialog" title="编辑歌单" width="500px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="歌单封面">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".jpg,.jpeg,.png,.gif,.webp"
            :on-change="handleEditCoverChange"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <el-image v-if="editForm.coverUrl" :src="editForm.coverUrl" style="width: 80px; height: 80px; margin-top: 8px" fit="cover" />
        </el-form-item>
        <el-form-item label="歌单名称" required>
          <el-input v-model="editForm.name" placeholder="请输入歌单名称" />
        </el-form-item>
        <el-form-item label="歌单描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="简单描述一下你的歌单" />
        </el-form-item>
        <el-form-item label="公开设置">
          <el-radio-group v-model="editForm.isPublic">
            <el-radio :value="1">公开</el-radio>
            <el-radio :value="0">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="歌单分类">
          <el-input v-model="editForm.category" placeholder="如：华语、流行、摇滚、电子、民谣..." maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserPlaylists, createPlaylist, updatePlaylist, deletePlaylist, uploadCover } from '@/api/song'
import { useUserStore } from '@/stores/user'
import { usePlaylistStore } from '@/stores/playlist'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const playlistStore = usePlaylistStore()

const playlists = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const creating = ref(false)
const editing = ref(false)
const coverFile = ref(null)
const editCoverFile = ref(null)

const createForm = ref({
  name: '',
  description: '',
  isPublic: 1,
  category: ''
})

const editForm = ref({
  id: null,
  name: '',
  description: '',
  isPublic: 1,
  coverUrl: null,
  category: ''
})

onMounted(async () => {
  await loadPlaylists()
})

const loadPlaylists = async () => {
  if (!userStore.userInfo?.userId) return
  loading.value = true
  try {
    const res = await getUserPlaylists(userStore.userInfo.userId)
    playlists.value = res.data || []
  } catch (e) {
    ElMessage.error('获取歌单失败')
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/playlist/${id}`)
}

const handleCoverChange = (file) => {
  coverFile.value = file.raw
}

const handleEditCoverChange = (file) => {
  editCoverFile.value = file.raw
}

const handleCreate = async () => {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入歌单名称')
    return
  }
  creating.value = true
  try {
    let coverUrl = null
    if (coverFile.value) {
      const coverRes = await uploadCover(coverFile.value)
      coverUrl = coverRes.data
    }
    await createPlaylist({
      name: createForm.value.name,
      description: createForm.value.description,
      isPublic: createForm.value.isPublic,
      category: createForm.value.category || undefined,
      userId: userStore.userInfo.userId,
      coverUrl
    })
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    coverFile.value = null
    createForm.value = { name: '', description: '', isPublic: 1, category: '' }
    await loadPlaylists()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

const handleEdit = (pl) => {
  editForm.value = {
    id: pl.id,
    name: pl.name,
    description: pl.description || '',
    isPublic: pl.isPublic,
    coverUrl: pl.coverUrl,
    category: pl.category || ''
  }
  editCoverFile.value = null
  showEditDialog.value = true
}

const handleSaveEdit = async () => {
  if (!editForm.value.name.trim()) {
    ElMessage.warning('请输入歌单名称')
    return
  }
  editing.value = true
  try {
    let coverUrl = editForm.value.coverUrl
    if (editCoverFile.value) {
      const coverRes = await uploadCover(editCoverFile.value)
      coverUrl = coverRes.data
    }
    await updatePlaylist(editForm.value.id, {
      name: editForm.value.name,
      description: editForm.value.description,
      isPublic: editForm.value.isPublic,
      coverUrl,
      category: editForm.value.category || undefined
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    await loadPlaylists()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    editing.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个歌单吗？', '提示', { type: 'warning' })
    await deletePlaylist(id)
    playlistStore.removePlaylist(id)
    ElMessage.success('删除成功')
    await loadPlaylists()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.my-playlists {
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

.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.playlist-card {
  background: var(--bg-secondary);
  border-radius: 8px;
  overflow: hidden;
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
  cursor: pointer;
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
  cursor: pointer;
}

.playlist-name:hover {
  color: var(--primary-color);
}

.playlist-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-actions {
  display: flex;
  gap: 8px;
}

.playlist-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.playlist-action-btn:hover {
  color: var(--primary-color);
}

.playlist-action-btn.delete:hover {
  color: var(--accent-pink);
}
</style>
