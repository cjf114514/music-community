import { ref } from 'vue'

const frequencyData = ref(new Uint8Array(128))
const waveformData = ref(new Uint8Array(128))
let lastAnalyser = null
let animId = null

function pollData() {
  if (lastAnalyser) {
    lastAnalyser.getByteFrequencyData(frequencyData.value)
    lastAnalyser.getByteTimeDomainData(waveformData.value)
  }
  animId = requestAnimationFrame(pollData)
}

export function setAnalyser(analyser) {
  lastAnalyser = analyser
  if (analyser && !animId) {
    pollData()
  }
}

export function clearAnalyser() {
  lastAnalyser = null
  if (animId) {
    cancelAnimationFrame(animId)
    animId = null
  }
}

export function useAudioAnalyser() {
  return { frequencyData, waveformData }
}
