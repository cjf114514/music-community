<template>
  <div class="account-settings-page">
    <h2 class="page-title">
      <el-icon><Back /></el-icon>
      <span @click="router.back()" class="back-link">返回个人中心</span>
    </h2>

    <div class="settings-sections">
      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            <span>修改密码</span>
          </div>
        </template>
        <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">确认修改</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
            <span>绑定/修改邮箱</span>
          </div>
        </template>
        <div class="current-email" v-if="currentEmail">
          <span class="label">当前邮箱：</span>
          <span class="value">{{ currentEmail }}</span>
        </div>
        <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef" label-width="100px">
          <el-form-item label="新邮箱" prop="newEmail">
            <el-input v-model="emailForm.newEmail" placeholder="请输入新的QQ邮箱" />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="code-row">
              <el-input v-model="emailForm.code" placeholder="请输入验证码" />
              <el-button :disabled="emailCountdown > 0 || !isQQEmail" @click="handleSendBindCode">
                {{ emailCountdown > 0 ? `${emailCountdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="emailLoading" @click="handleChangeEmail">确认修改</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword, changeEmail, sendBindCode, getUserInfo } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const pwdFormRef = ref(null)
const emailFormRef = ref(null)
const pwdLoading = ref(false)
const emailLoading = ref(false)
const emailCountdown = ref(0)
const currentEmail = ref('')
let emailTimer = null

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const emailForm = reactive({
  newEmail: '',
  code: ''
})

const isQQEmail = computed(() => /^[1-9]\d{4,10}@qq\.com$/.test(emailForm.newEmail))

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const emailRules = {
  newEmail: [
    { required: true, message: '请输入QQ邮箱', trigger: 'blur' },
    { pattern: /^[1-9]\d{4,10}@qq\.com$/, message: '请输入正确的QQ邮箱', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

onMounted(async () => {
  try {
    const res = await getUserInfo()
    currentEmail.value = res.data.email || ''
  } catch (e) {}
})

const handleChangePassword = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  pwdLoading.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '密码修改失败')
  } finally {
    pwdLoading.value = false
  }
}

const handleSendBindCode = async () => {
  if (!isQQEmail.value) {
    ElMessage.warning('请输入正确的QQ邮箱')
    return
  }
  try {
    await sendBindCode(emailForm.newEmail)
    ElMessage.success('验证码已发送')
    emailCountdown.value = 60
    emailTimer = setInterval(() => {
      emailCountdown.value--
      if (emailCountdown.value <= 0) {
        clearInterval(emailTimer)
        emailTimer = null
      }
    }, 1000)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发送失败')
  }
}

const handleChangeEmail = async () => {
  const valid = await emailFormRef.value.validate().catch(() => false)
  if (!valid) return

  emailLoading.value = true
  try {
    await changeEmail({
      newEmail: emailForm.newEmail,
      code: emailForm.code
    })
    ElMessage.success('邮箱修改成功')
    currentEmail.value = emailForm.newEmail
    emailForm.newEmail = ''
    emailForm.code = ''
    const res = await getUserInfo()
    userStore.setUserInfo({ ...userStore.userInfo, ...res.data })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '邮箱修改失败')
  } finally {
    emailLoading.value = false
  }
}
</script>

<style scoped>
.account-settings-page {
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

.settings-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-weight: 600;
}

.card-header svg {
  color: var(--primary-color);
}

.current-email {
  margin-bottom: 20px;
  padding: 12px 16px;
  background: var(--bg-elevated);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.current-email .label {
  color: var(--text-secondary);
  font-size: 14px;
}

.current-email .value {
  color: var(--primary-color);
  font-weight: 500;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}
</style>
