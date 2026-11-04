<template>
  <div class="forgot-container">
    <div class="forgot-bg-effects">
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
      <div class="grid-lines"></div>
    </div>
    <div class="forgot-card">
      <div class="forgot-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
        </div>
        <h2>找回密码</h2>
        <p>通过绑定邮箱重置密码</p>
      </div>

      <el-steps :active="step" align-center class="reset-steps" finish-status="success">
        <el-step title="验证邮箱" />
        <el-step title="重置密码" />
        <el-step title="完成" />
      </el-steps>

      <div v-if="step === 0" class="step-content">
        <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef" label-width="0">
          <el-form-item prop="email">
            <div class="input-group">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
              <el-input v-model="emailForm.email" placeholder="请输入绑定的QQ邮箱" size="large" />
            </div>
          </el-form-item>
          <el-form-item prop="code">
            <div class="code-row">
              <div class="input-group captcha-input">
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>
                <el-input v-model="emailForm.code" placeholder="邮箱验证码" size="large" />
              </div>
              <button type="button" class="code-btn" :disabled="countdown > 0 || !isQQEmail" @click="handleSendCode">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </button>
            </div>
          </el-form-item>
          <el-form-item>
            <button type="button" class="submit-btn" @click="handleVerifyEmail">下一步</button>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="step === 1" class="step-content">
        <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="0">
          <el-form-item prop="newPassword">
            <div class="input-group">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              <el-input v-model="pwdForm.newPassword" type="password" placeholder="新密码（6-20位）" size="large" show-password />
            </div>
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <div class="input-group">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="确认新密码" size="large" show-password />
            </div>
          </el-form-item>
          <el-form-item>
            <button type="button" class="submit-btn" :disabled="loading" @click="handleResetPassword">
              <span v-if="!loading">重置密码</span>
              <span v-else class="loading-dots">...</span>
            </button>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="step === 2" class="step-content success-content">
        <div class="success-icon">
          <svg viewBox="0 0 24 24" fill="currentColor" width="48" height="48"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
        </div>
        <h3>密码重置成功</h3>
        <p>请使用新密码登录</p>
        <button type="button" class="submit-btn" @click="$router.push('/login')">前往登录</button>
      </div>

      <div class="forgot-footer">
        <span class="link" @click="$router.push('/login')">返回登录</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendResetCode, resetPassword } from '@/api/auth'

const router = useRouter()
const step = ref(0)
const loading = ref(false)
const countdown = ref(0)
const emailFormRef = ref(null)
const pwdFormRef = ref(null)
let timer = null

const emailForm = reactive({
  email: '',
  code: ''
})

const pwdForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const isQQEmail = computed(() => /^[1-9]\d{4,10}@qq\.com$/.test(emailForm.email))

const emailRules = {
  email: [
    { required: true, message: '请输入QQ邮箱', trigger: 'blur' },
    { pattern: /^[1-9]\d{4,10}@qq\.com$/, message: '请输入正确的QQ邮箱', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleSendCode = async () => {
  if (!isQQEmail.value) {
    ElMessage.warning('请输入正确的QQ邮箱')
    return
  }
  try {
    await sendResetCode(emailForm.email)
    ElMessage.success('验证码已发送')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发送失败')
  }
}

const handleVerifyEmail = async () => {
  const valid = await emailFormRef.value.validate().catch(() => false)
  if (!valid) return
  step.value = 1
}

const handleResetPassword = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await resetPassword({
      email: emailForm.email,
      code: emailForm.code,
      newPassword: pwdForm.newPassword
    })
    step.value = 2
    ElMessage.success('密码重置成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #07070e;
  position: relative;
  overflow: hidden;
}

.forgot-bg-effects {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}

.glow-1 {
  width: 350px;
  height: 350px;
  background: rgba(99, 102, 241, 0.12);
  top: -80px;
  right: -80px;
  animation: float1 8s ease-in-out infinite;
}

.glow-2 {
  width: 300px;
  height: 300px;
  background: rgba(34, 211, 238, 0.08);
  bottom: -60px;
  left: -60px;
  animation: float2 10s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-25px, 25px); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -20px); }
}

.grid-lines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

.forgot-card {
  width: 460px;
  padding: 40px;
  background: rgba(15, 15, 26, 0.8);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  box-shadow: var(--neon-glow);
  position: relative;
  z-index: 1;
}

.forgot-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--gradient-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 16px;
  box-shadow: var(--neon-glow-sm);
}

.forgot-header h2 {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.forgot-header p {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
}

.reset-steps {
  margin-bottom: 28px;
}

.step-content {
  min-height: 180px;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 0 14px;
  transition: all 0.3s;
  width: 100%;
}

.input-group:focus-within {
  border-color: var(--primary-color);
  box-shadow: var(--neon-glow-sm);
}

.input-group svg {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.input-group :deep(.el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  padding: 0 !important;
}

.input-group :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-size: 14px;
}

.input-group :deep(.el-input__inner::placeholder) {
  color: var(--text-tertiary) !important;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.code-btn {
  flex-shrink: 0;
  height: 40px;
  padding: 0 16px;
  border: 1px solid var(--border-glow);
  border-radius: 12px;
  background: var(--nav-hover-bg);
  color: var(--primary-color);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  font-family: inherit;
}

.code-btn:hover:not(:disabled) {
  background: var(--nav-active-bg);
  border-color: var(--primary-color);
}

.code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: var(--text-tertiary);
  border-color: var(--border-color);
}

.submit-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: var(--gradient-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: var(--neon-glow-sm);
  font-family: inherit;
}

.submit-btn:hover:not(:disabled) {
  box-shadow: var(--neon-glow);
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-dots {
  animation: blink 1.4s infinite both;
}

@keyframes blink {
  0%, 20% { opacity: 0; }
  50% { opacity: 1; }
  80%, 100% { opacity: 0; }
}

.success-content {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  color: #22c55e;
  margin-bottom: 16px;
}

.success-content h3 {
  color: var(--text-primary);
  margin: 0 0 8px;
  font-size: 20px;
}

.success-content p {
  color: var(--text-secondary);
  margin: 0 0 24px;
}

.forgot-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
}

.forgot-footer .link {
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
}

.forgot-footer .link:hover {
  color: var(--primary-hover);
}
</style>
