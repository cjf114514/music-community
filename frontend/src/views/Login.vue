<template>
  <div class="login-container">
    <div class="login-bg-effects">
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
      <div class="glow glow-3"></div>
      <div class="grid-lines"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
        </div>
        <h2>NEON<span class="accent">MUSIC</span></h2>
        <p>沉浸式音乐体验</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <el-input v-model="form.username" placeholder="用户名" size="large" />
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="handleLogin" />
          </div>
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <div class="input-group captcha-input">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>
              <el-input v-model="form.captchaCode" placeholder="验证码" size="large" @keyup.enter="handleLogin" />
            </div>
            <img :src="captchaUrl" class="captcha-img" @click="refreshCaptcha" />
          </div>
        </el-form-item>
        <el-form-item>
          <button type="button" class="login-btn" :disabled="loading" @click="handleLogin">
            <span v-if="!loading">登 录</span>
            <span v-else class="loading-dots">...</span>
          </button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>还没有账号？</span>
        <span class="link" @click="$router.push('/register')">立即注册</span>
        <span class="divider">|</span>
        <span class="link" @click="$router.push('/forgot-password')">忘记密码</span>
      </div>
      <div class="guest-entry">
        <div class="guest-divider">
          <span class="guest-divider-line"></span>
          <span class="guest-divider-text">或者</span>
          <span class="guest-divider-line"></span>
        </div>
        <button class="guest-btn" @click="enterAsGuest">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          游客模式进入
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { setGuestMode } from '@/api/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const captchaKey = ref('')
const captchaUrl = ref('')

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

onMounted(() => {
  refreshCaptcha()
})

const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

const refreshCaptcha = () => {
  captchaKey.value = generateUUID()
  captchaUrl.value = `/api/captcha/generate?key=${captchaKey.value}&t=${Date.now()}`
}

const enterAsGuest = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  setGuestMode(true)
  router.push('/')
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    //</>发送请求给 SpringBoot 后端 /login 接口
    //...form 表单数据:用户名、密码、验证码
    const res = await login({ ...form, captchaKey: captchaKey.value })
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data)
    setGuestMode(false)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
    refreshCaptcha()
    form.captchaCode = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #07070e;
  position: relative;
  overflow: hidden;
}

.login-bg-effects {
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
  width: 400px;
  height: 400px;
  background: rgba(99, 102, 241, 0.15);
  top: -100px;
  left: -100px;
  animation: float1 8s ease-in-out infinite;
}

.glow-2 {
  width: 300px;
  height: 300px;
  background: rgba(139, 92, 246, 0.12);
  bottom: -80px;
  right: -80px;
  animation: float2 10s ease-in-out infinite;
}

.glow-3 {
  width: 200px;
  height: 200px;
  background: rgba(34, 211, 238, 0.08);
  top: 50%;
  left: 60%;
  animation: float3 6s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, 30px); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-20px, -20px); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-15px, 15px); }
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

.login-card {
  width: 420px;
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

.login-header {
  text-align: center;
  margin-bottom: 32px;
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

.login-header h2 {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 3px;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.login-header h2 .accent {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-header p {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
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

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  height: 40px;
  cursor: pointer;
  border-radius: 10px;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
  transition: border-color 0.2s;
}

.captcha-img:hover {
  border-color: var(--border-glow);
}

.login-btn {
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

.login-btn:hover:not(:disabled) {
  box-shadow: var(--neon-glow);
  transform: translateY(-1px);
}

.login-btn:disabled {
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

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.login-footer .link {
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
}

.login-footer .link:hover {
  color: var(--primary-hover);
}

.login-footer .divider {
  color: var(--border-color);
  margin: 0 8px;
}

.guest-entry {
  margin-top: 24px;
}

.guest-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.guest-divider-line {
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.guest-divider-text {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.guest-btn {
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;
}

.guest-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: rgba(var(--primary-rgb, 102, 126, 234), 0.05);
}
</style>
