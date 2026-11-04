<template>
  <canvas
    ref="canvasRef"
    class="visualizer-canvas"
    :width="width"
    :height="height"
    @click="$emit('toggle')"
  />
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  analyserNode: { type: Object, default: null },
  width: { type: Number, default: 280 },
  height: { type: Number, default: 48 },
  type: { type: String, default: 'frequency' }
})

defineEmits(['toggle'])

const canvasRef = ref(null)
let ctx = null
let animId = null

function draw() {
  if (!canvasRef.value || !props.analyserNode) return
  if (!ctx) ctx = canvasRef.value.getContext('2d')

  const bufferLength = props.analyserNode.frequencyBinCount
  const dataArray = new Uint8Array(bufferLength)

  if (props.type === 'waveform') {
    props.analyserNode.getByteTimeDomainData(dataArray)
  } else {
    props.analyserNode.getByteFrequencyData(dataArray)
  }

  ctx.clearRect(0, 0, props.width, props.height)

  const barWidth = (props.width / bufferLength) * 2.2
  let x = 0

  for (let i = 0; i < bufferLength; i++) {
    const value = dataArray[i] / 255
    const barHeight = value * props.height

    const hue = 240 + (i / bufferLength) * 60
    ctx.fillStyle = `hsla(${hue}, 80%, 60%, 0.85)`

    if (props.type === 'waveform') {
      const y = (props.height - barHeight) / 2
      ctx.fillRect(x, y, barWidth - 1, Math.max(barHeight, 2))
    } else {
      ctx.fillRect(x, props.height - barHeight, barWidth - 1, barHeight)
    }
    x += barWidth + 1
  }

  animId = requestAnimationFrame(draw)
}

watch(() => props.analyserNode, (node) => {
  if (animId) cancelAnimationFrame(animId)
  if (node) draw()
})

onMounted(() => {
  if (props.analyserNode) draw()
})

onUnmounted(() => {
  if (animId) cancelAnimationFrame(animId)
})
</script>

<style scoped>
.visualizer-canvas {
  display: block;
  border-radius: 6px;
  cursor: pointer;
  width: 100%;
}
</style>
