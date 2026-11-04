<template>
  <canvas ref="canvasRef" class="dynamic-bg-canvas" />
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'

const canvasRef = ref(null)
const themeStore = useThemeStore()
let animId = null
let ctx = null
let w = 0
let h = 0
let time = 0

const WAVE_COUNT = 4
const BAR_COUNT = 64

function drawLightMode() {
  ctx.clearRect(0, 0, w, h)
  const colors = [
    [99, 102, 248],
    [139, 92, 246],
    [34, 211, 238]
  ]
  for (let i = 0; i < WAVE_COUNT; i++) {
    const baseY = h * (0.45 + i * 0.12)
    const amp = 30 + i * 12
    const freq = 0.003 - i * 0.0005
    const speed = 0.6 + i * 0.2
    const alpha = 0.06 + i * 0.02
    const [r, g, b] = colors[i % colors.length]
    ctx.beginPath()
    ctx.moveTo(0, h)
    for (let x = 0; x <= w; x += 3) {
      const y = baseY + Math.sin(x * freq + time * speed) * amp + Math.sin(x * freq * 2 + time * speed * 1.5) * (amp * 0.35)
      ctx.lineTo(x, y)
    }
    ctx.lineTo(w, h)
    ctx.closePath()
    const grad = ctx.createLinearGradient(0, baseY - amp, 0, h)
    grad.addColorStop(0, `rgba(${r},${g},${b},${alpha})`)
    grad.addColorStop(1, `rgba(${r},${g},${b},0)`)
    ctx.fillStyle = grad
    ctx.fill()
  }
}

function drawDarkMode() {
  ctx.clearRect(0, 0, w, h)
  const barWidth = Math.max(w / BAR_COUNT, 4)
  const gap = 2
  for (let i = 0; i < BAR_COUNT; i++) {
    const x = i * (barWidth + gap)
    const n1 = Math.sin(i * 0.15 + time * 0.7)
    const n2 = Math.sin(i * 0.08 + time * 0.4 + 1)
    const n3 = Math.sin(i * 0.25 + time * 1.1 + 2)
    const barH = Math.max(h * (0.08 + ((n1 + n2 + n3) / 3) * 0.22), 4)
    const y = h - barH
    const hue = 240 + (i / BAR_COUNT) * 60 + Math.sin(time * 0.3 + i * 0.05) * 20
    const lightness = 50 + Math.sin(time * 0.5 + i * 0.08) * 15
    const alpha = 0.12 + Math.abs(n1) * 0.12
    ctx.beginPath()
    const r = Math.min(barWidth / 2, barH / 2)
    ctx.moveTo(x + r, y)
    ctx.lineTo(x + barWidth - r, y)
    ctx.arcTo(x + barWidth, y, x + barWidth, y + r, r)
    ctx.lineTo(x + barWidth, y + barH - r)
    ctx.arcTo(x + barWidth, y + barH, x + barWidth - r, y + barH, r)
    ctx.lineTo(x + r, y + barH)
    ctx.arcTo(x, y + barH, x, y + barH - r, r)
    ctx.lineTo(x, y + r)
    ctx.arcTo(x, y, x + r, y, r)
    ctx.closePath()
    const grad = ctx.createLinearGradient(x, y, x, h)
    grad.addColorStop(0, `hsla(${hue},80%,${lightness}%,${alpha})`)
    grad.addColorStop(1, `hsla(${hue},80%,${lightness}%,0)`)
    ctx.fillStyle = grad
    ctx.fill()
  }
  for (let i = 0; i < 2; i++) {
    const baseY = h * (0.55 + i * 0.18)
    const amp = 18 + i * 10
    const freq = 0.004 - i * 0.001
    const speed = 0.5 + i * 0.25
    const hue = i === 0 ? 260 : 190
    ctx.beginPath()
    ctx.moveTo(0, h)
    for (let x = 0; x <= w; x += 3) {
      const y = baseY + Math.sin(x * freq + time * speed) * amp + Math.sin(x * freq * 2.2 + time * speed * 1.3) * (amp * 0.4)
      ctx.lineTo(x, y)
    }
    ctx.lineTo(w, h)
    ctx.closePath()
    const grad = ctx.createLinearGradient(0, baseY - amp, 0, h)
    grad.addColorStop(0, `hsla(${hue},75%,60%,0.06)`)
    grad.addColorStop(1, `hsla(${hue},75%,60%,0)`)
    ctx.fillStyle = grad
    ctx.fill()
  }
}

function animate() {
  time += 0.016
  if (themeStore.isDark) {
    drawDarkMode()
  } else {
    drawLightMode()
  }
  animId = requestAnimationFrame(animate)
}

function resize() {
  if (!canvasRef.value) return
  w = window.innerWidth
  h = window.innerHeight
  canvasRef.value.width = w
  canvasRef.value.height = h
}

onMounted(() => {
  if (!canvasRef.value) return
  ctx = canvasRef.value.getContext('2d')
  resize()
  animate()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.dynamic-bg-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 0;
}
</style>
