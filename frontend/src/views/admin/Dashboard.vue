<template>
  <div class="dashboard">
    <div class="stats-row">
      <div class="stat-card" v-for="item in statCards" :key="item.key">
        <div class="stat-card-inner">
          <div class="stat-icon-wrap" :style="{ background: item.gradient }">
            <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path :d="item.icon" /></svg>
          </div>
          <div class="stat-detail">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card wide">
        <div class="card-header">
          <h3>播放趋势</h3>
          <div class="header-actions">
            <button v-for="period in ['7天', '30天']" :key="period"
              :class="['period-btn', { active: selectedPeriod === period }]"
              @click="changePeriod(period)">{{ period }}</button>
          </div>
        </div>
        <div class="chart-body" ref="playTrendRef">
          <div class="chart-empty" v-if="!dailyPlays.length">暂无播放数据</div>
        </div>
      </div>

      <div class="chart-card">
        <div class="card-header">
          <h3>内容分布</h3>
        </div>
        <div class="chart-body" ref="contentDistRef"></div>
      </div>
    </div>

    <div class="bottom-row">
      <div class="chart-card wide">
        <div class="card-header">
          <h3>热门歌曲 TOP 10</h3>
        </div>
        <div class="top-songs">
          <div class="song-item" v-for="(song, idx) in topSongs" :key="song.id">
            <span class="song-rank" :class="{ top3: idx < 3 }">{{ idx + 1 }}</span>
            <div class="song-cover" v-if="song.coverUrl">
              <img :src="song.coverUrl" alt="" />
            </div>
            <div class="song-cover placeholder" v-else>
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
            </div>
            <div class="song-info">
              <span class="song-name">{{ song.name }}</span>
              <span class="song-artist">{{ song.singer }}</span>
            </div>
            <div class="song-plays">{{ formatCount(song.playCount) }}</div>
            <div class="song-bar-wrap">
              <div class="song-bar" :style="{ width: getBarWidth(song.playCount) }"></div>
            </div>
          </div>
          <div class="empty-state" v-if="!topSongs.length">暂无歌曲数据</div>
        </div>
      </div>

      <div class="chart-card">
        <div class="card-header">
          <h3>近期动态</h3>
        </div>
        <div class="activity-list">
          <div class="activity-item" v-for="(act, idx) in activities" :key="idx">
            <div class="activity-dot" :style="{ background: act.color }"></div>
            <div class="activity-content">
              <span class="activity-text">{{ act.text }}</span>
              <span class="activity-time">{{ act.time }}</span>
            </div>
          </div>
          <div class="empty-state" v-if="!activities.length">暂无动态</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const selectedPeriod = ref('30天')
const playTrendRef = ref(null)
const contentDistRef = ref(null)
let playTrendChart = null
let contentDistChart = null

const dailyPlays = ref([])
const topSongs = ref([])
const activities = ref([])
const statCards = ref([])

const formatCount = (n) => {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

const getBarWidth = (count) => {
  const max = topSongs.value.length ? Math.max(topSongs.value[0].playCount || 0, 1) : 1
  return Math.max(5, ((count || 0) / max) * 100) + '%'
}

const changePeriod = (period) => {
  selectedPeriod.value = period
  renderPlayTrendChart()
}

const renderPlayTrendChart = () => {
  if (!playTrendRef.value) return
  const data = dailyPlays.value
  const days = selectedPeriod.value === '7天' ? 7 : 30
  const sliced = data.slice(-days)

  if (playTrendChart) playTrendChart.dispose()
  playTrendChart = echarts.init(playTrendRef.value)

  if (!sliced.length) return

  const option = {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(26,26,46,0.9)', borderColor: 'rgba(102,126,234,0.3)', textStyle: { color: '#e0e0e0' } },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: sliced.map(d => d.date?.slice(5) || ''),
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { color: 'rgba(255,255,255,0.4)', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: 'rgba(255,255,255,0.4)' }
    },
    series: [{
      data: sliced.map(d => d.count),
      type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#667eea', width: 3 },
      itemStyle: { color: '#667eea', borderWidth: 2, borderColor: '#fff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(102,126,234,0.4)' },
          { offset: 1, color: 'rgba(102,126,234,0.02)' }
        ])
      }
    }]
  }
  playTrendChart.setOption(option)
}

const initContentDistChart = (d) => {
  if (!contentDistRef.value) return
  if (contentDistChart) contentDistChart.dispose()
  contentDistChart = echarts.init(contentDistRef.value)

  const pieData = [
    { value: d.songCount || 0, name: '歌曲', itemStyle: { color: '#667eea' } },
    { value: d.singerCount || 0, name: '歌手', itemStyle: { color: '#f5576c' } },
    { value: d.playlistCount || 0, name: '歌单', itemStyle: { color: '#4facfe' } },
    { value: d.postCount || 0, name: '帖子', itemStyle: { color: '#f093fb' } },
    { value: d.commentCount || 0, name: '评论', itemStyle: { color: '#43e97b' } }
  ].filter(item => item.value > 0)

  if (!pieData.length) {
    pieData.push({ value: 1, name: '暂无数据', itemStyle: { color: 'rgba(255,255,255,0.1)' } })
  }

  const option = {
    tooltip: { backgroundColor: 'rgba(26,26,46,0.9)', borderColor: 'rgba(102,126,234,0.3)', textStyle: { color: '#e0e0e0' } },
    series: [{
      type: 'pie', radius: ['45%', '70%'], center: ['50%', '50%'],
      label: { color: 'rgba(255,255,255,0.6)', fontSize: 12 },
      data: pieData
    }]
  }
  contentDistChart.setOption(option)
}

const loadDashboard = async () => {
  try {
    const res = await request.get('/admin/dashboard')
    const d = res.data

    statCards.value = [
      {
        key: 'users', label: '用户总数', value: d.userCount || 0,
        icon: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
        gradient: 'linear-gradient(135deg, #667eea, #764ba2)'
      },
      {
        key: 'songs', label: '歌曲总数', value: d.songCount || 0,
        icon: 'M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z',
        gradient: 'linear-gradient(135deg, #f093fb, #f5576c)'
      },
      {
        key: 'playlists', label: '歌单总数', value: d.playlistCount || 0,
        icon: 'M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z',
        gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)'
      },
      {
        key: 'plays', label: '累计播放', value: d.totalPlayCount || d.todayPlayCount || 0,
        icon: 'M8 5v14l11-7z',
        gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)'
      },
      {
        key: 'posts', label: '社区帖子', value: d.postCount || 0,
        icon: 'M21 6h-2v9H6v2c0 .55.45 1 1 1h11l4 4V7c0-.55-.45-1-1-1zm-4 6V3c0-.55-.45-1-1-1H3c-.55 0-1 .45-1 1v14l4-4h10c.55 0 1-.45 1-1z',
        gradient: 'linear-gradient(135deg, #f093fb, #f5576c)'
      },
      {
        key: 'comments', label: '评论总数', value: d.commentCount || 0,
        icon: 'M21 15h2v2h-2v-2zm0-4h2v2h-2v-2zm2 8h-2v2c1.1 0 2-.9 2-2zM13 3h2v2h-2V3zm8 0h2v2h-2V3zM1 17h2v2H1v-2zm0-4h2v2H1v-2zm0-4h2v2H1V9zm4 12h2v2H5v-2zm0-16h2v2H5V3zM1 21c0 1.1.9 2 2 2h2v-2H1v-2zm0-18h2V1H3c-1.1 0-2 .9-2 2v2h2V3zm12 18h2v2h-2v-2zm4 0h2v2h-2v-2z',
        gradient: 'linear-gradient(135deg, #667eea, #43e97b)'
      }
    ]

    topSongs.value = d.topSongs || []
    activities.value = d.recentActivities || []
    dailyPlays.value = d.chartData?.dailyPlays || []

    await nextTick()
    renderPlayTrendChart()
    initContentDistChart(d)
  } catch (e) {
    console.error('Dashboard load error:', e)
  }
}

const handleResize = () => {
  playTrendChart?.resize()
  contentDistChart?.resize()
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  playTrendChart?.dispose()
  contentDistChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  background: rgba(26, 26, 46, 0.6);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
}

.stat-card:hover {
  border-color: rgba(102,126,234,0.3);
  transform: translateY(-2px);
}

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: rgba(255,255,255,0.4);
  margin-top: 2px;
}

.charts-row, .bottom-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.chart-card {
  background: rgba(26, 26, 46, 0.6);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 0;
}

.card-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255,255,255,0.8);
}

.header-actions {
  display: flex;
  gap: 4px;
}

.period-btn {
  padding: 4px 12px;
  border: none;
  background: transparent;
  color: rgba(255,255,255,0.4);
  font-size: 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.period-btn.active {
  background: rgba(102,126,234,0.2);
  color: #667eea;
}

.period-btn:hover {
  color: rgba(255,255,255,0.7);
}

.chart-body {
  height: 260px;
  padding: 8px;
  position: relative;
}

.chart-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: rgba(255,255,255,0.2);
  font-size: 14px;
}

.top-songs {
  padding: 12px 20px;
  max-height: 320px;
  overflow-y: auto;
}

.top-songs::-webkit-scrollbar { width: 4px; }
.top-songs::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 2px; }

.song-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.song-item:last-child { border-bottom: none; }

.song-rank {
  width: 20px;
  font-size: 14px;
  font-weight: 700;
  color: rgba(255,255,255,0.3);
  text-align: center;
}

.song-rank.top3 {
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.song-cover {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.song-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.song-cover.placeholder {
  background: rgba(255,255,255,0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255,255,255,0.2);
}

.song-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.song-name {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-artist {
  font-size: 11px;
  color: rgba(255,255,255,0.3);
}

.song-plays {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
  min-width: 50px;
  text-align: right;
}

.song-bar-wrap {
  width: 80px;
  height: 4px;
  background: rgba(255,255,255,0.06);
  border-radius: 2px;
  overflow: hidden;
}

.song-bar {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 2px;
  transition: width 0.6s ease;
}

.activity-list {
  padding: 12px 20px;
  max-height: 320px;
  overflow-y: auto;
}

.activity-list::-webkit-scrollbar { width: 4px; }
.activity-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 2px; }

.activity-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.activity-item:last-child { border-bottom: none; }

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.activity-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.activity-text {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  line-height: 1.4;
}

.activity-time {
  font-size: 11px;
  color: rgba(255,255,255,0.3);
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: rgba(255,255,255,0.2);
  font-size: 14px;
}
</style>
