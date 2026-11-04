<template>
  <div class="daily-recommend-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="page-title">
          <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28" style="color:var(--primary-color)"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-5.5-2.5l7.51-3.49L17.5 6.5 9.99 9.99 6.5 17.5zm5.5-6.6c.61 0 1.1.49 1.1 1.1s-.49 1.1-1.1 1.1-1.1-.49-1.1-1.1.49-1.1 1.1-1.1z"/></svg>
          每日推荐
        </h2>
        <p class="page-desc">基于你的听歌历史、收藏偏好和相似用户，AI 为你精选今日好歌</p>
      </div>
      <el-button type="primary" @click="handleRefresh" :loading="loading" class="refresh-btn">
        <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
        换一批
      </el-button>
    </div>

    <div v-if="taste" class="taste-panel">
      <div class="taste-header">
        <h3>你的音乐品味</h3>
        <div class="taste-tags" v-if="taste.tasteTags?.length">
          <span class="taste-tag" v-for="tag in taste.tasteTags" :key="tag">{{ tag }}</span>
        </div>
      </div>
      <div class="taste-stats">
        <div class="taste-stat">
          <div class="taste-stat-value">{{ taste.totalPlays || 0 }}</div>
          <div class="taste-stat-label">累计播放</div>
        </div>
        <div class="taste-stat">
          <div class="taste-stat-value">{{ taste.totalFavorites || 0 }}</div>
          <div class="taste-stat-label">收藏歌曲</div>
        </div>
        <div class="taste-stat">
          <div class="taste-stat-value">{{ taste.topSingers?.length || 0 }}</div>
          <div class="taste-stat-label">喜爱歌手</div>
        </div>
      </div>
      <div class="taste-detail">
        <div class="taste-section" v-if="taste.topSingers?.length">
          <h4>最常听的歌手</h4>
          <div class="taste-list">
            <div class="taste-list-item" v-for="(s, i) in taste.topSingers" :key="s.name">
              <span class="taste-rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
              <span class="taste-name">{{ s.name }}</span>
              <div class="taste-bar-wrap">
                <div class="taste-bar" :style="{ width: getTasteBarWidth(s.count, taste.topSingers[0]?.count || 1) }"></div>
              </div>
              <span class="taste-count">{{ s.count }}</span>
            </div>
          </div>
        </div>
        <div class="taste-section" v-if="taste.playDistribution?.length">
          <h4>听歌时段分布</h4>
          <div class="taste-chart" ref="tasteChartRef"></div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-wrapper">
      <svg viewBox="0 0 24 24" fill="currentColor" width="40" height="40" class="loading-icon" style="color:var(--primary-color)"><path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
      <span>AI 正在为你精选推荐...</span>
    </div>

    <template v-else>
      <div v-if="recommendSongs.length" class="recommend-grid">
        <div
          v-for="(song, index) in recommendSongs"
          :key="song.id"
          class="song-card"
          @click="playSong(song)"
        >
          <div class="card-cover">
            <img v-if="song.coverUrl" :src="song.coverUrl" />
            <div v-else class="cover-placeholder" :style="{ background: getGradient(index) }">
              <svg viewBox="0 0 24 24" fill="currentColor" width="50" height="50"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
            </div>
            <div class="play-overlay">
              <svg viewBox="0 0 24 24" fill="currentColor" width="40" height="40" color="#fff"><path d="M8 5v14l11-7z"/></svg>
            </div>
            <div class="add-queue-overlay" @click.stop="handleAddToQueue(song)" title="添加到播放列表">
              <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" color="#fff"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
            </div>
          </div>
          <div class="card-info">
            <div class="card-name" @click.stop="$router.push(`/song/${song.id}`)">{{ song.name }}</div>
            <div class="card-singer">{{ song.singer }}</div>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无推荐，先去听听歌吧" />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useMusicStore } from '@/stores/music'
import { ElMessage } from 'element-plus'
import { getDailyRecommendations, getTasteAnalysis } from '@/api/recommend'
import * as echarts from 'echarts'

const musicStore = useMusicStore()
const loading = ref(true)
const recommendSongs = ref([])
const taste = ref(null)
const tasteChartRef = ref(null)
let tasteChart = null

const gradients = [
  'linear-gradient(135deg, #ec4141 0%, #ff6b6b 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
]

onMounted(async () => {
  await Promise.all([loadRecommendations(), loadTaste()])
})

onBeforeUnmount(() => {
  tasteChart?.dispose()
})

const loadRecommendations = async () => {
  loading.value = true
  try {
    const res = await getDailyRecommendations(20)
    recommendSongs.value = res.data || []
  } catch (e) {} finally {
    loading.value = false
  }
}

const loadTaste = async () => {
  try {
    const res = await getTasteAnalysis()
    taste.value = res.data
    if (taste.value?.playDistribution?.length) {
      await nextTick()
      renderTasteChart()
    }
  } catch (e) {}
}

const renderTasteChart = () => {
  if (!tasteChartRef.value || !taste.value?.playDistribution?.length) return
  tasteChart?.dispose()
  tasteChart = echarts.init(tasteChartRef.value)

  const data = taste.value.playDistribution
  const periods = [
    { name: '凌晨', hours: [0,1,2,3,4,5] },
    { name: '早晨', hours: [6,7,8,9,10,11] },
    { name: '下午', hours: [12,13,14,15,16,17] },
    { name: '晚上', hours: [18,19,20,21,22,23] },
  ]

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26,26,46,0.9)',
      borderColor: 'rgba(102,126,234,0.3)',
      textStyle: { color: '#e0e0e0', fontSize: 12 },
      formatter: (params) => {
        const h = params[0].dataIndex
        return `${h}:00 - ${h}:59<br/>播放 ${params[0].value} 次`
      }
    },
    grid: { left: 35, right: 10, top: 10, bottom: 25 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.hour + ':00'),
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: {
        color: 'rgba(255,255,255,0.4)',
        fontSize: 10,
        interval: 3
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: 'rgba(255,255,255,0.4)', fontSize: 10 }
    },
    series: [{
      data: data.map(d => d.count),
      type: 'bar',
      barWidth: '60%',
      itemStyle: {
        color: (params) => {
          const h = params.dataIndex
          if (h >= 22 || h < 6) return new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }])
          if (h >= 6 && h < 12) return new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#43e97b' }, { offset: 1, color: '#38f9d7' }])
          if (h >= 12 && h < 18) return new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#4facfe' }, { offset: 1, color: '#00f2fe' }])
          return new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#f093fb' }, { offset: 1, color: '#f5576c' }])
        },
        borderRadius: [3, 3, 0, 0]
      }
    }]
  }
  tasteChart.setOption(option)
}

const handleRefresh = () => {
  loadRecommendations()
}

const playSong = (song) => {
  musicStore.setCurrentSong(song)
}

const handleAddToQueue = (song) => {
  const added = musicStore.addToQueue(song)
  if (added) {
    ElMessage.success('已添加到播放列表')
  } else {
    ElMessage.info('歌曲已在播放列表中')
  }
}

const getGradient = (index) => {
  return gradients[index % gradients.length]
}

const getTasteBarWidth = (count, max) => {
  return Math.max(8, (count / max) * 100) + '%'
}

window.addEventListener('resize', () => tasteChart?.resize())
</script>

<style scoped>
.daily-recommend-page {
  padding: 0 0 40px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.page-desc {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.refresh-btn {
  background: var(--gradient-primary) !important;
  border: none !important;
  display: flex;
  align-items: center;
  gap: 6px;
}

.refresh-btn:hover {
  opacity: 0.9;
}

.taste-panel {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 28px;
}

.taste-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.taste-header h3 {
  color: var(--text-primary);
  font-size: 18px;
  margin: 0;
}

.taste-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.taste-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background: linear-gradient(135deg, rgba(102,126,234,0.2), rgba(118,75,162,0.2));
  color: var(--primary-color);
  border: 1px solid rgba(102,126,234,0.3);
}

.taste-stats {
  display: flex;
  gap: 32px;
  margin-bottom: 24px;
}

.taste-stat {
  text-align: center;
}

.taste-stat-value {
  font-size: 28px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.taste-stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.taste-detail {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.taste-section h4 {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0 0 12px;
  font-weight: 500;
}

.taste-list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}

.taste-rank {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  background: var(--bg-elevated);
  flex-shrink: 0;
}

.taste-rank.top {
  background: var(--gradient-primary);
  color: #fff;
}

.taste-name {
  font-size: 13px;
  color: var(--text-primary);
  width: 80px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}

.taste-bar-wrap {
  flex: 1;
  height: 6px;
  background: var(--bg-elevated);
  border-radius: 3px;
  overflow: hidden;
}

.taste-bar {
  height: 100%;
  background: var(--gradient-primary);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.taste-count {
  font-size: 12px;
  color: var(--text-tertiary);
  width: 30px;
  text-align: right;
  flex-shrink: 0;
}

.taste-chart {
  height: 160px;
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
  gap: 16px;
  color: var(--text-secondary);
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 24px;
}

.song-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.song-card:hover {
  transform: translateY(-4px);
}

.card-cover {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: var(--card-shadow);
  transition: box-shadow 0.2s;
}

.song-card:hover .card-cover {
  box-shadow: var(--card-hover-shadow);
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.song-card:hover .play-overlay {
  opacity: 1;
}

.add-queue-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s;
  cursor: pointer;
}

.add-queue-overlay:hover {
  background: rgba(102, 126, 234, 0.8);
  transform: scale(1.1);
}

.song-card:hover .add-queue-overlay {
  opacity: 1;
}

.card-info {
  padding: 12px 4px 0;
}

.card-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-primary);
  margin-bottom: 4px;
  cursor: pointer;
}

.card-name:hover {
  color: var(--primary-color);
}

.card-singer {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
