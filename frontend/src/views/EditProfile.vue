<template>
  <div class="edit-profile-page">
    <h2 class="page-title">
      <el-icon><Back /></el-icon>
      <span @click="router.back()" class="back-link">返回个人中心</span>
    </h2>

    <el-card class="edit-card">
      <template #header>编辑资料</template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <div style="display:flex;align-items:center;gap:12px;width:100%">
            <el-input :model-value="form.email" disabled />
            <el-button type="primary" link @click="router.push('/account-settings')">修改邮箱</el-button>
          </div>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input v-model="form.signature" type="textarea" :rows="3" placeholder="写一句话介绍自己吧" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleUpdate">保存</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo, updateUserInfo } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  gender: 0,
  signature: ''
})

onMounted(async () => {
  try {
    const res = await getUserInfo()
    const info = res.data
    form.nickname = info.nickname || ''
    form.email = info.email || ''
    form.gender = info.gender ?? 0
    form.signature = info.signature || ''
  } catch (e) {
    ElMessage.error('获取用户信息失败')
  }
})

const handleUpdate = async () => {
  loading.value = true
  try {
    const res = await updateUserInfo(form)
    userStore.setUserInfo({ ...userStore.userInfo, ...res.data })
    ElMessage.success('保存成功')
    router.back()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.edit-profile-page {
  padding: 20px 0;
  max-width: 700px;
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.back-link {
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: normal;
}

.back-link:hover {
  color: var(--primary-color);
}

.edit-card {
  margin-top: 20px;
}
</style>
