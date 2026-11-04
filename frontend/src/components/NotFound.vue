<template>
  <div class="not-found">
    <div class="nf-icon">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="56" height="56">
        <circle cx="11" cy="11" r="7" />
        <path d="M20 20l-3.5-3.5" stroke-linecap="round" />
      </svg>
    </div>
    <h2 class="nf-title">{{ title }}</h2>
    <p class="nf-sub">{{ subtitle }}</p>
    <div class="nf-actions">
      <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

/**
 * 通用的「资源不存在」空状态
 *
 * 后端对失效资源返回 HTTP 404 之后，详情页不再是一片空白，
 * 而是给出明确原因 + 两个出口（回首页 / 回上一页）。
 */
defineProps({
  title: { type: String, default: '内容不存在' },
  subtitle: { type: String, default: '它可能已被删除，或者链接已经失效' }
})

const router = useRouter()
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 80px 24px;
  min-height: 60vh;
  color: rgba(255, 255, 255, 0.55);
}
.nf-icon {
  color: rgba(255, 255, 255, 0.18);
  margin-bottom: 18px;
}
.nf-title {
  font-size: 20px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 10px;
}
.nf-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 26px;
  line-height: 1.7;
}
.nf-actions { display: flex; gap: 12px; }
</style>
