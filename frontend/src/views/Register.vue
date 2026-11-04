<template>
  <div class="register-container">
    <div class="register-bg-effects">
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
      <div class="glow glow-3"></div>
      <div class="grid-lines"></div>
    </div>
    <div class="register-card">
      <div class="register-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
        </div>
        <h2>创建账号</h2>
        <p>加入 NEON<span class="accent">MUSIC</span></p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <el-input v-model="form.username" placeholder="用户名（3-20位）" size="large" />
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            <el-input v-model="form.password" type="password" placeholder="密码（6-20位）" size="large" show-password />
          </div>
        </el-form-item>
        <el-form-item prop="email">
          <div class="email-row">
            <div class="input-group">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
              <el-input v-model="form.email" placeholder="QQ邮箱" size="large" />
            </div>
            <button type="button" class="code-btn" :disabled="countdown > 0 || !isQQEmail" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </div>
        </el-form-item>
        <el-form-item prop="emailCode">
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>
            <el-input v-model="form.emailCode" placeholder="邮箱验证码" size="large" />
          </div>
        </el-form-item>
        <el-form-item prop="nickname">
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"/></svg>
            <el-input v-model="form.nickname" placeholder="昵称（选填）" size="large" />
          </div>
        </el-form-item>
        <el-form-item>
          <button type="button" class="register-btn" :disabled="loading" @click="handleRegister">
            <span v-if="!loading">注 册</span>
            <span v-else class="loading-dots">...</span>
          </button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        <span>已有账号？</span>
        <span class="link" @click="$router.push('/login')">返回登录</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendEmailCode } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const countdown = ref(0)
let timer = null

const form = reactive({
  username: '',
  password: '',
  email: '',
  emailCode: '',
  nickname: ''
})

const isQQEmail = computed(() => {
  return /^[1-9]\d{4,10}@qq\.com$/.test(form.email)
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入QQ邮箱', trigger: 'blur' },
    { pattern: /^[1-9]\d{4,10}@qq\.com$/, message: '请输入正确的QQ邮箱', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

const sendCode = async () => {
  if (!isQQEmail.value) {
    ElMessage.warning('请输入正确的QQ邮箱')
    return
  }
  try {
    await sendEmailCode(form.email)
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

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #07070e;
  position: relative;
  overflow: hidden;
}

.register-bg-effects {
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
  background: rgba(139, 92, 246, 0.15);
  top: -80px;
  right: -80px;
  animation: float1 8s ease-in-out infinite;
}

.glow-2 {
  width: 300px;
  height: 300px;
  background: rgba(244, 114, 182, 0.1);
  bottom: -60px;
  left: -60px;
  animation: float2 10s ease-in-out infinite;
}

.glow-3 {
  width: 200px;
  height: 200px;
  background: rgba(34, 211, 238, 0.08);
  top: 40%;
  left: 30%;
  animation: float3 6s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-25px, 25px); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -20px); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(15px, -10px); }
}

.grid-lines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(139, 92, 246, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(139, 92, 246, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

.register-card {
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

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--gradient-warm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 16px;
  box-shadow: var(--neon-glow-sm);
}

.register-header h2 {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.register-header p {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
}

.register-header p .accent {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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

.email-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.email-row .input-group {
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

.register-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: var(--gradient-warm);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 0 15px rgba(244, 114, 182, 0.2);
  font-family: inherit;
}

.register-btn:hover:not(:disabled) {
  box-shadow: 0 0 25px rgba(244, 114, 182, 0.3), 0 0 50px rgba(139, 92, 246, 0.15);
  transform: translateY(-1px);
}

.register-btn:disabled {
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

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.register-footer .link {
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
}

.register-footer .link:hover {
  color: var(--primary-hover);
}
</style>
