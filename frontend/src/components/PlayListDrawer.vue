<template>
  <div>
    <div v-if="store.showPlayList" class="overlay" @click="store.closePlayList"></div>
    
    <div v-if="store.showPlayList" class="drawer">
      <div class="drawer-header">
        <div class="header-left">
          <span class="header-title">播放队列</span>
          <span class="header-count">{{ store.queue.length }}首</span>
        </div>
        <div class="header-right">
          <button @click="store.toggleQueueMode" class="header-btn mode-btn" :class="{ active: store.isQueueMode }">
            <el-icon :size="14"><Sort /></el-icon>
            <span>{{ store.isQueueMode ? '队列顺序' : '常规模式' }}</span>
          </button>
          <button @click="clearAll" class="header-btn" v-if="store.queue.length > 0">
            <el-icon :size="14"><Delete /></el-icon>
            <span>清空</span>
          </button>
          <button @click="store.closePlayList" class="header-btn close-btn">
            <el-icon :size="14"><Close /></el-icon>
          </button>
        </div>
      </div>

      <div class="drawer-content">
        <div v-if="store.queue.length === 0" class="empty">
          <el-icon :size="48" color="rgba(255,255,255,0.15)"><Music /></el-icon>
          <p class="empty-text">队列是空的</p>
          <p class="empty-hint">去发现好音乐吧</p>
        </div>
        
        <div v-else class="song-list" ref="listRef">
          <div 
            v-for="(song, index) in store.queue" 
            :key="song.id"
            class="song-item"
            :class="{ 
              active: store.currentSong?.id === song.id,
              dragging: dragIndex === index
            }"
            draggable="true"
            @dragstart="onDragStart($event, index)"
            @dragover.prevent="onDragOver($event, index)"
            @dragenter.prevent="onDragEnter(index)"
            @dragend="onDragEnd"
            @click="playSong(song, index)"
          >
            <div class="song-drag" @mousedown.stop>
              <el-icon :size="14"><Rank /></el-icon>
            </div>
            <div class="song-index">
              <span v-if="store.currentSong?.id === song.id && store.isPlaying" class="playing-icon">
                <span class="bar"></span><span class="bar"></span><span class="bar"></span>
              </span>
              <span v-else class="index-num">{{ index + 1 }}</span>
            </div>
            <div class="song-cover">
              <img v-if="song.coverUrl" :src="song.coverUrl" />
              <div v-else class="cover-placeholder">
                <el-icon :size="14"><Music /></el-icon>
              </div>
            </div>
            <div class="song-info">
              <div class="song-name">{{ song.name }}</div>
              <div class="song-singer">{{ song.singer }}</div>
            </div>
            <div class="song-remove" @click.stop="removeSong(index)">
              <el-icon :size="14"><Close /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useMusicStore } from '@/stores/music'

const store = useMusicStore()
const dragIndex = ref(null)
const dragOverIndex = ref(null)

const playSong = (song, index) => {
  store.currentSongIndex = index
  store.setCurrentSong(song)
}

const removeSong = (index) => {
  store.removeFromQueue(index)
}

const clearAll = () => {
  store.clearQueue()
}

const onDragStart = (e, index) => {
  dragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', index)
  if (e.target) {
    e.target.style.opacity = '0.5'
  }
}

const onDragOver = (e, index) => {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

const onDragEnter = (index) => {
  if (dragIndex.value !== null && dragIndex.value !== index) {
    dragOverIndex.value = index
    store.reorderQueue(dragIndex.value, index)
    dragIndex.value = index
  }
}

const onDragEnd = (e) => {
  dragIndex.value = null
  dragOverIndex.value = null
  if (e.target) {
    e.target.style.opacity = '1'
  }
}
</script>

<style scoped>
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  backdrop-filter: blur(2px);
}

.drawer {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 76px;
  width: 380px;
  background: var(--bg-secondary, #0f0f1a);
  z-index: 10000;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.5);
  border-left: 1px solid var(--border-color, rgba(99, 102, 241, 0.12));
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color, rgba(99, 102, 241, 0.12));
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #e2e8f0);
}

.header-count {
  font-size: 13px;
  color: var(--text-tertiary, #64748b);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  background: transparent;
  color: var(--text-secondary, #94a3b8);
  transition: all 0.2s;
}

.header-btn:hover {
  background: var(--nav-hover-bg, rgba(99, 102, 241, 0.08));
  color: var(--text-primary, #e2e8f0);
}

.close-btn:hover {
  color: #f56c6c;
}

.mode-btn.active {
  color: var(--primary-color, #818cf8);
  background: var(--primary-light, rgba(129, 140, 248, 0.12));
}

.drawer-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.drawer-content::-webkit-scrollbar {
  width: 4px;
}

.drawer-content::-webkit-scrollbar-track {
  background: transparent;
}

.drawer-content::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb, rgba(99, 102, 241, 0.3));
  border-radius: 2px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 8px;
}

.empty-text {
  font-size: 15px;
  color: var(--text-tertiary, #64748b);
  margin-top: 12px;
}

.empty-hint {
  font-size: 13px;
  color: var(--text-tertiary, #64748b);
  opacity: 0.6;
}

.song-list {
  padding: 0;
}

.song-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.song-item:hover {
  background: var(--nav-hover-bg, rgba(99, 102, 241, 0.08));
}

.song-item.active {
  background: var(--primary-light, rgba(129, 140, 248, 0.12));
}

.song-item.active .song-name {
  color: var(--primary-color, #818cf8);
}

.song-item.active .index-num {
  display: none;
}

.song-item.dragging {
  opacity: 0.4;
  background: var(--nav-active-bg, rgba(99, 102, 241, 0.15));
}

.song-drag {
  width: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary, #64748b);
  cursor: grab;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
}

.song-item:hover .song-drag {
  opacity: 1;
}

.song-drag:active {
  cursor: grabbing;
}

.song-index {
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.index-num {
  font-size: 13px;
  color: var(--text-tertiary, #64748b);
  font-variant-numeric: tabular-nums;
}

.playing-icon {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 14px;
  justify-content: center;
}

.playing-icon .bar {
  width: 3px;
  background: var(--primary-color, #818cf8);
  border-radius: 1px;
  animation: playing-bar 0.8s ease-in-out infinite;
}

.playing-icon .bar:nth-child(1) { height: 50%; animation-delay: 0s; }
.playing-icon .bar:nth-child(2) { height: 100%; animation-delay: 0.15s; }
.playing-icon .bar:nth-child(3) { height: 35%; animation-delay: 0.3s; }

@keyframes playing-bar {
  0%, 100% { transform: scaleY(0.4); }
  50% { transform: scaleY(1); }
}

.song-cover {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-elevated, #161625);
}

.song-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #a78bfa));
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
}

.song-info {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.song-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary, #e2e8f0);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.15s;
}

.song-singer {
  font-size: 12px;
  color: var(--text-tertiary, #64748b);
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-remove {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--text-tertiary, #64748b);
  cursor: pointer;
  flex-shrink: 0;
  opacity: 0;
  transition: all 0.15s;
}

.song-item:hover .song-remove {
  opacity: 1;
}

.song-remove:hover {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}
</style>
