<template>
  <div class="messages-page">
    <div class="messages-layout">
      <div :class="['conv-panel', { hidden: activeConv }]">
        <div class="conv-header">
          <h2>私信</h2>
          <span v-if="totalUnread > 0" class="conv-unread-badge">{{ totalUnread }}</span>
        </div>
        <div class="conv-list">
          <div v-if="convLoading" class="conv-loading">加载中...</div>
          <div v-else-if="!conversations.length" class="conv-empty">
            <svg viewBox="0 0 24 24" fill="currentColor" width="48" height="48"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/></svg>
            <p>暂无私信会话</p>
          </div>
          <div
            v-for="conv in conversations"
            :key="conv.id"
            :class="['conv-item', { active: activeConv?.id === conv.id }]"
            @click="openConversation(conv)"
          >
            <el-avatar :size="44" :src="conv.otherAvatar">
              {{ conv.otherNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="conv-info">
              <div class="conv-top">
                <span class="conv-name">{{ conv.otherNickname || '用户' }}</span>
                <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
              </div>
              <div class="conv-preview">{{ conv.lastMessageContent || '' }}</div>
            </div>
            <span v-if="getConvUnread(conv) > 0" class="conv-dot">{{ getConvUnread(conv) }}</span>
          </div>
        </div>
      </div>

      <div :class="['chat-panel', { show: activeConv }]">
        <template v-if="activeConv">
          <div class="chat-header">
            <div class="chat-back" @click="closeConversation">
              <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
            </div>
            <div class="chat-user-info">
              <el-avatar :size="32" :src="activeConv.otherAvatar">
                {{ activeConv.otherNickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="chat-username">{{ activeConv.otherNickname || '用户' }}</span>
            </div>
            <div class="chat-actions">
              <div class="chat-action-btn" @click="showConvMenu = !showConvMenu">
                <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
              </div>
              <div v-if="showConvMenu" class="chat-menu" @click.stop>
                <div class="chat-menu-item" @click="goToProfile">查看主页</div>
                <div class="chat-menu-item danger" @click="handleBlockFromChat">
                  {{ isBlocked ? '取消拉黑' : '拉黑用户' }}
                </div>
              </div>
            </div>
          </div>

          <div class="chat-messages" ref="messagesRef">
            <div v-if="msgLoading" class="msg-loading">加载中...</div>
            <div v-if="hasMore" class="load-more" @click="loadMoreMessages">加载更多</div>
            <div
              v-for="msg in displayMessages"
              :key="msg.id"
              :class="['msg-row', { mine: Number(msg.senderId) === myId }]"
            >
              <el-avatar v-if="Number(msg.senderId) !== myId" :size="32" :src="msg.senderAvatar">
                {{ msg.senderNickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="msg-bubble-wrap">
                <div
                  :class="['msg-bubble', { recalled: msg.isRecalled }]"
                  @contextmenu.prevent="onMsgContext($event, msg)"
                >
                  <template v-if="msg.isRecalled">该消息已撤回</template>
                  <template v-else>{{ msg.content }}</template>
                </div>
                <div class="msg-time">{{ formatMsgTime(msg.createTime) }}</div>
              </div>
              <el-avatar v-if="Number(msg.senderId) === myId" :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
            </div>
          </div>

          <div class="stranger-tip" v-if="strangerRemaining >= 0">
            陌生人可发送 {{ strangerRemaining }} 条私信，关注对方后可无限发送
            <span class="follow-link" @click="followFromChat">关注</span>
          </div>

          <div class="chat-input-area">
            <el-input
              v-model="inputText"
              placeholder="输入消息..."
              @keyup.enter="handleSend"
              :disabled="!canSend"
              maxlength="500"
              show-word-limit
            />
            <el-button type="primary" @click="handleSend" :disabled="!inputText.trim() || !canSend" :loading="sending">
              发送
            </el-button>
          </div>
        </template>
        <div v-else class="chat-placeholder">
          <svg viewBox="0 0 24 24" fill="currentColor" width="64" height="64"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/></svg>
          <p>选择一个会话开始聊天</p>
        </div>
      </div>
    </div>

    <div v-if="contextMenu.show" class="context-menu" :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }">
      <div v-if="Number(contextMenu.msg?.senderId) === myId && !contextMenu.msg?.isRecalled" class="ctx-item" @click="handleRecall">撤回</div>
      <div class="ctx-item" @click="handleCopy">复制</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, triggerRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  getConversations,
  getMessages,
  sendMessage,
  markConversationRead,
  getMessageUnreadCount,
  recallMessage,
  canSendMessage
} from '@/api/message'
import { blockUser, unblockUser, checkBlockStatus, followUser, checkFollowStatus } from '@/api/social'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const myId = computed(() => Number(userStore.userInfo?.userId) || 0)

const conversations = ref([])
const convLoading = ref(false)
const totalUnread = ref(0)
const activeConv = ref(null)
const messages = ref([])
const msgLoading = ref(false)
const hasMore = ref(false)
const currentPage = ref(0)
const inputText = ref('')
const sending = ref(false)
const canSend = ref(true)
const strangerRemaining = ref(-1)
const isBlocked = ref(false)
const isFollowingUser = ref(false)
const showConvMenu = ref(false)
const messagesRef = ref(null)

const contextMenu = ref({ show: false, x: 0, y: 0, msg: null })

const displayMessages = computed(() => messages.value)

let pollTimer = null

onMounted(async () => {
  await loadConversations()
  if (route.query.to) {
    await startConversationWith(Number(route.query.to))
  }
  pollTimer = setInterval(pollUnread, 15000)
  document.addEventListener('click', hideContextMenu)
})

onUnmounted(() => {
  clearInterval(pollTimer)
  document.removeEventListener('click', hideContextMenu)
})

const loadConversations = async () => {
  convLoading.value = true
  try {
    const res = await getConversations()
    conversations.value = res.data || []
    calcTotalUnread()
  } catch (e) {}
  finally { convLoading.value = false }
}

const calcTotalUnread = () => {
  let count = 0
  conversations.value.forEach(c => { count += getConvUnread(c) })
  totalUnread.value = count
}

const getConvUnread = (conv) => {
  if (!myId.value) return 0
  return Number(conv.user1Id) === myId.value ? conv.user1Unread : conv.user2Unread
}

const openConversation = async (conv) => {
  activeConv.value = conv
  showConvMenu.value = false
  currentPage.value = 0
  messages.value = []
  await loadMessages()
  await markConversationRead(conv.id)
  const c = conversations.value.find(x => x.id === conv.id)
  if (c) {
    if (Number(c.user1Id) === myId.value) c.user1Unread = 0
    else c.user2Unread = 0
    calcTotalUnread()
  }
  await checkChatStatus()
  nextTick(() => scrollToBottom())
}

const startConversationWith = async (userId) => {
  const existing = conversations.value.find(c => c.otherUserId === userId)
  if (existing) {
    await openConversation(existing)
    return
  }
  const { getOrCreateConversation } = await import('@/api/message')
  try {
    const res = await getOrCreateConversation(userId)
    await loadConversations()
    const conv = conversations.value.find(c => c.otherUserId === userId)
    if (conv) await openConversation(conv)
  } catch (e) {
    ElMessage.error('无法创建会话')
  }
}

const loadMessages = async () => {
  if (!activeConv.value) return
  msgLoading.value = true
  try {
    const res = await getMessages(activeConv.value.id, currentPage.value, 20)
    const page = res.data
    messages.value = [...(page.content || []).reverse()]
    hasMore.value = !page.last
  } catch (e) {}
  finally { msgLoading.value = false }
}

const loadMoreMessages = async () => {
  currentPage.value++
  const oldHeight = messagesRef.value?.scrollHeight || 0
  try {
    const res = await getMessages(activeConv.value.id, currentPage.value, 20)
    const page = res.data
    const olderMessages = (page.content || []).reverse()
    messages.value = [...olderMessages, ...messages.value]
    hasMore.value = !page.last
    await nextTick()
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight - oldHeight
    }
  } catch (e) {}
}

const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || !activeConv.value || sending.value) return
  sending.value = true
  const tempId = 'temp_' + Date.now()
  const tempMsg = {
    id: tempId,
    conversationId: activeConv.value.id,
    senderId: myId.value,
    receiverId: Number(activeConv.value.otherUserId),
    content: text,
    messageType: 'TEXT',
    isRead: false,
    isRecalled: false,
    createTime: new Date().toISOString(),
    senderNickname: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
    senderAvatar: userStore.userInfo?.avatar || ''
  }
  messages.value = [...messages.value, tempMsg]
  inputText.value = ''
  await nextTick()
  scrollToBottom()
  try {
    const res = await sendMessage(activeConv.value.otherUserId, text)
    const realMsg = res.data
    const idx = messages.value.findIndex(m => m.id === tempId)
    if (idx >= 0) {
      messages.value[idx] = {
        id: realMsg.id,
        conversationId: realMsg.conversationId,
        senderId: Number(realMsg.senderId),
        receiverId: Number(realMsg.receiverId),
        content: realMsg.content || text,
        messageType: realMsg.messageType || 'TEXT',
        isRead: false,
        isRecalled: false,
        createTime: realMsg.createTime || new Date().toISOString(),
        senderNickname: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
        senderAvatar: userStore.userInfo?.avatar || ''
      }
      triggerRef(messages)
    }
    await loadConversations()
    await checkChatStatus()
    nextTick(() => scrollToBottom())
  } catch (e) {
    messages.value = messages.value.filter(m => m.id !== tempId)
    triggerRef(messages)
    inputText.value = text
    ElMessage.error(e.response?.data?.message || '发送失败')
  }
  finally { sending.value = false }
}

const checkChatStatus = async () => {
  if (!activeConv.value) return
  try {
    const [blockRes, followRes, canRes] = await Promise.all([
      checkBlockStatus(activeConv.value.otherUserId),
      checkFollowStatus(activeConv.value.otherUserId),
      canSendMessage(activeConv.value.otherUserId)
    ])
    isBlocked.value = blockRes.data
    isFollowingUser.value = followRes.data
    canSend.value = canRes.data.canSend
    strangerRemaining.value = canRes.data.remaining
  } catch (e) {}
}

const handleBlockFromChat = async () => {
  try {
    if (isBlocked.value) {
      await unblockUser(activeConv.value.otherUserId)
      isBlocked.value = false
      ElMessage.success('已取消拉黑')
    } else {
      await blockUser(activeConv.value.otherUserId)
      isBlocked.value = true
      canSend.value = false
      ElMessage.success('已拉黑')
    }
    showConvMenu.value = false
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const followFromChat = async () => {
  try {
    await followUser(activeConv.value.otherUserId)
    isFollowingUser.value = true
    canSend.value = true
    strangerRemaining.value = -1
    ElMessage.success('关注成功')
  } catch (e) {
    ElMessage.error('关注失败')
  }
}

const goToProfile = () => {
  if (activeConv.value) {
    router.push(`/user/${activeConv.value.otherUserId}`)
  }
  showConvMenu.value = false
}

const closeConversation = () => {
  activeConv.value = null
  showConvMenu.value = false
}

const onMsgContext = (e, msg) => {
  contextMenu.value = { show: true, x: e.clientX, y: e.clientY, msg }
}

const hideContextMenu = () => {
  contextMenu.value.show = false
}

const handleRecall = async () => {
  const msg = contextMenu.value.msg
  if (!msg) return
  try {
    await recallMessage(msg.id)
    msg.isRecalled = true
    msg.content = '该消息已撤回'
  } catch (e) {
    ElMessage.error('撤回失败')
  }
  hideContextMenu()
}

const handleCopy = () => {
  const msg = contextMenu.value.msg
  if (msg?.content) {
    navigator.clipboard.writeText(msg.content).catch(() => {})
  }
  hideContextMenu()
}

const pollUnread = async () => {
  try {
    const res = await getMessageUnreadCount()
    totalUnread.value = res.data?.count || 0
  } catch (e) {}
}

const scrollToBottom = () => {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return d.toLocaleDateString()
}

const formatMsgTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>

<style scoped>
.messages-page {
  height: calc(100vh - 140px);
  min-height: 500px;
}

.messages-layout {
  display: flex;
  height: 100%;
  background: var(--bg-secondary);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.conv-panel {
  width: 320px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.conv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border-color);
}

.conv-header h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.conv-unread-badge {
  min-width: 20px;
  height: 20px;
  line-height: 20px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: #ef4444;
  border-radius: 10px;
  text-align: center;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
}

.conv-loading,
.conv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-tertiary);
  gap: 12px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.conv-item:hover {
  background: var(--nav-hover-bg);
}

.conv-item.active {
  background: rgba(99, 102, 241, 0.08);
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conv-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.conv-time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.conv-preview {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 4px;
}

.conv-dot {
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 5px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: var(--primary-color);
  border-radius: 9px;
  text-align: center;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-tertiary);
  gap: 16px;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-color);
}

.chat-back {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
}

.chat-back:hover {
  background: var(--nav-hover-bg);
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.chat-username {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.chat-actions {
  position: relative;
}

.chat-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
}

.chat-action-btn:hover {
  background: var(--nav-hover-bg);
}

.chat-menu {
  position: absolute;
  top: 36px;
  right: 0;
  width: 140px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  box-shadow: var(--card-hover-shadow);
  z-index: 50;
  overflow: hidden;
}

.chat-menu-item {
  padding: 10px 16px;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  transition: background 0.15s;
}

.chat-menu-item:hover {
  background: var(--nav-hover-bg);
}

.chat-menu-item.danger {
  color: #ef4444;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.msg-loading,
.load-more {
  text-align: center;
  padding: 8px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.load-more {
  cursor: pointer;
}

.load-more:hover {
  color: var(--primary-color);
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.msg-row.mine {
  flex-direction: row-reverse;
}

.msg-bubble-wrap {
  max-width: 60%;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.msg-row.mine .msg-bubble {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-purple));
  color: #fff;
  border: none;
}

.light .msg-row.mine .msg-bubble {
  background: linear-gradient(135deg, var(--primary-deep), #5b21b6);
}

.msg-bubble.recalled {
  color: var(--text-tertiary);
  font-style: italic;
  background: transparent;
  border: 1px dashed var(--border-color);
}

.msg-row.mine .msg-bubble.recalled {
  background: transparent;
  color: var(--text-tertiary);
  border: 1px dashed var(--border-color);
}

.msg-time {
  font-size: 10px;
  color: var(--text-tertiary);
  margin-top: 4px;
  text-align: right;
}

.msg-row:not(.mine) .msg-time {
  text-align: left;
}

.stranger-tip {
  padding: 8px 20px;
  font-size: 12px;
  color: var(--accent-amber);
  background: rgba(245, 158, 11, 0.06);
  text-align: center;
}

.follow-link {
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 600;
  margin-left: 4px;
}

.follow-link:hover {
  text-decoration: underline;
}

.chat-input-area {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
}

.chat-input-area :deep(.el-input) {
  flex: 1;
}

.context-menu {
  position: fixed;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: var(--card-hover-shadow);
  z-index: 999;
  overflow: hidden;
  min-width: 100px;
}

.ctx-item {
  padding: 8px 16px;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  transition: background 0.15s;
}

.ctx-item:hover {
  background: var(--nav-hover-bg);
}

@media (max-width: 768px) {
  .conv-panel {
    width: 100%;
  }

  .conv-panel.hidden {
    display: none;
  }

  .chat-panel {
    display: none;
  }

  .chat-panel.show {
    display: flex;
  }

  .chat-back {
    display: flex;
  }
}
</style>
