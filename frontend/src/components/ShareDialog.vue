<template>
  <el-dialog v-model="visible" title="分享" width="400px" :append-to-body="true" class="share-dialog">
    <div class="share-content">
      <div class="share-card">
        <div class="share-cover">
          <img v-if="coverUrl" :src="coverUrl" />
          <div v-else class="cover-placeholder">
            <el-icon :size="40"><Music /></el-icon>
          </div>
        </div>
        <div class="share-info">
          <div class="share-title">{{ title }}</div>
          <div class="share-subtitle">{{ subtitle }}</div>
        </div>
      </div>
      <div class="share-actions">
        <div class="share-btn" @click="copyLink">
          <el-icon :size="24"><Link /></el-icon>
          <span>复制链接</span>
        </div>
        <div class="share-btn" @click="showQRCode = true">
          <el-icon :size="24"><FullScreen /></el-icon>
          <span>二维码</span>
        </div>
      </div>
    </div>

    <el-dialog v-model="showQRCode" title="二维码" width="260px" :append-to-body="true" class="qrcode-dialog">
      <div class="qrcode-wrapper">
        <canvas ref="qrCanvas"></canvas>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'

const props = defineProps({
  modelValue: Boolean,
  type: String,
  id: Number,
  title: String,
  subtitle: String,
  coverUrl: String,
  shareToken: String
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const showQRCode = ref(false)
const qrCanvas = ref(null)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const shareUrl = ref('')

watch(() => props.shareToken, async (token) => {
  if (token) {
    const baseUrl = window.location.origin
    shareUrl.value = props.type === 'song'
      ? `${baseUrl}/shared-song/${token}`
      : `${baseUrl}/shared-playlist/${token}`
  }
})

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('链接已复制到剪贴板')
  } catch (e) {
    const input = document.createElement('input')
    input.value = shareUrl.value
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('链接已复制到剪贴板')
  }
}

watch(showQRCode, async (val) => {
  if (val && shareUrl.value) {
    await nextTick()
    if (qrCanvas.value) {
      QRCode.toCanvas(qrCanvas.value, shareUrl.value, { width: 200, margin: 1 }, (error) => {
        if (error) console.error(error)
      })
    }
  }
})
</script>

<style scoped>
.share-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.share-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-primary);
  border-radius: 8px;
}

.share-cover {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.share-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
}

.share-info {
  flex: 1;
  overflow: hidden;
}

.share-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.share-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.share-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.share-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  border-radius: 8px;
  background: var(--bg-primary);
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-primary);
  font-size: 12px;
}

.share-btn:hover {
  background: var(--nav-hover-bg);
  color: var(--primary-color);
}

.qrcode-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

:deep(.qrcode-dialog .el-dialog__body) {
  padding: 10px 20px;
}
</style>
