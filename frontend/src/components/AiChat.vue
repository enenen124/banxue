<script setup>
import { ref, nextTick, onUnmounted } from 'vue'
import { marked } from 'marked'
import { Icon } from '@iconify/vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const open = ref(false)
const messages = ref([
  { role: 'assistant', text: '你好！我是你的学习伴学助手 📚\n有什么学习问题都可以问我~', md: '' }
])
const input = ref('')
const loading = ref(false)
const bodyEl = ref(null)
const bubbleEl = ref(null)

// ========== 拖动逻辑 ==========
const bubbleX = ref(24)   // 气泡的 left 坐标
const bubbleY = ref(24)   // 气泡的 bottom 坐标
const dragging = ref(false)
let dragStartX = 0
let dragStartY = 0
let dragInitX = 24
let dragInitY = 24
let hasMoved = false        // 区分"拖动"和"点击"

// 按下鼠标 → 开始跟踪
const onDragStart = (e) => {
  dragging.value = true
  hasMoved = false
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragInitX = bubbleX.value
  dragInitY = bubbleY.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
}

// 移动鼠标 → 更新位置
const onDragMove = (e) => {
  if (!dragging.value) return
  const dx = dragStartX - e.clientX
  const dy = e.clientY - dragStartY
  if (Math.abs(dx) > 2 || Math.abs(dy) > 2) hasMoved = true
  bubbleX.value = Math.max(0, dragInitX - dx)
  bubbleY.value = Math.max(0, dragInitY - dy)
}

// 松开鼠标 → 结束拖动
const onDragEnd = () => {
  dragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

// 点击气泡（没拖动才打开）
const onBubbleClick = () => {
  if (!hasMoved) open.value = true
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
})

// 自动滚到底部
const scrollBottom = () => {
  nextTick(() => {
    if (bodyEl.value) bodyEl.value.scrollTop = bodyEl.value.scrollHeight
  })
}

// 发送消息
const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''
  messages.value.push({ role: 'user', text, md: '' })
  loading.value = true

  const API_BASE = window.location.hostname === 'localhost' ? '' : 'https://banxue-backend-klem.onrender.com'

  const idx = messages.value.length
  messages.value.push({ role: 'assistant', text: '', md: '' })
  scrollBottom()

  try {
    const token = localStorage.getItem('token')
    const response = await fetch(API_BASE + '/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ message: text })
    })

    if (!response.ok) {
      const err = await response.json()
      const msg = messages.value[idx]
      msg.text = err.message || '请求失败'
      msg.md = ''
      loading.value = false
      return
    }

    // 判断返回类型：SSE流 还是 普通JSON
    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('text/event-stream')) {
      // === SSE 流式响应 ===
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          const msg = messages.value[idx]
          msg.md = marked(msg.text)
          loading.value = false
          scrollBottom()
          break
        }
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop()

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed.startsWith('data:')) continue
          const payload = trimmed.slice(5).trim()
          if (!payload || payload === '[DONE]') continue
          try {
            const json = JSON.parse(payload)
            // 后端可能返回 {"reply":"..."} 或 OpenAI格式 {"choices":[{"delta":{"content":"..."}}]}
            let content = json.reply || json.choices?.[0]?.delta?.content || ''
            // 如果有 error 字段，显示错误
            if (json.error) content = '⚠️ ' + json.error
            if (content) {
              messages.value[idx].text += content
              messages.value[idx].md = marked(messages.value[idx].text)
              scrollBottom()
            }
          } catch { /* 跳过解析失败的行 */ }
        }
      }
    } else {
      // === 普通 JSON 响应 ===
      const data = await response.json()
      const msg = messages.value[idx]
      msg.text = data.reply || data.message || '(没有回复)'
      msg.md = marked(msg.text)
      loading.value = false
      scrollBottom()
    }
  } catch (e) {
    const msg = messages.value[idx]
    msg.text = '网络错误，请稍后重试'
    msg.md = ''
    loading.value = false
  }
}
</script>

<template>
  <!-- 浮动气泡（可拖动） -->
  <div
    v-if="!open"
    ref="bubbleEl"
    class="ai-bubble"
    :style="{ left: bubbleX + 'px', bottom: bubbleY + 'px' }"
    @mousedown="onDragStart"
    @click="onBubbleClick"
  >
    <Icon icon="fluent:bot-sparkle-20-filled" width="22" height="22" />
    <span class="bubble-text"></span>
  </div>

  <!-- 聊天窗 -->
  <div class="ai-chat" v-if="open">
    <div class="chat-header">
      <span><Icon icon="fluent:bot-sparkle-20-filled" width="18" height="18" /> 伴学AI</span>
      <button class="chat-close" @click="open = false">✕</button>
    </div>
    <div class="chat-body" ref="bodyEl">
      <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
        <!-- 用户消息：纯文本 -->
        <div v-if="msg.role === 'user'" class="msg-text">{{ msg.text }}</div>
        <!-- AI消息：markdown渲染 -->
        <div v-else class="msg-text markdown-body" v-html="msg.md || msg.text"></div>
      </div>
      <div v-if="loading" class="msg assistant">
        <div class="msg-text typing">思考中...</div>
      </div>
    </div>
    <div class="chat-footer">
      <input
        v-model="input"
        @keyup.enter="send"
        placeholder="输入你的学习问题..."
        :disabled="loading"
      />
      <button @click="send" :disabled="loading">发送</button>
    </div>
  </div>
</template>

<style scoped>
/* 浮动气泡 */
.ai-bubble {
  position: fixed;
  background: #1c1d1c;
  color: #fff;
  padding: 12px 16px;
  border-radius: 24px;
  cursor: grab;
  z-index: 9999;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.2);
  user-select: none;
  transition: box-shadow 0.2s;
}

.ai-bubble:hover { background: #333; box-shadow: 0 6px 20px rgba(0,0,0,0.3); }
.ai-bubble:active { cursor: grabbing; }

/* 聊天窗 */
.ai-chat {
  position: fixed;
  left: 20px;
  bottom: 20px;
  width: 340px;
  height: 460px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  z-index: 9999;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
  font-size: 14px;
}

.chat-close {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #999;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.msg { display: flex; }
.msg.user { justify-content: flex-end; }

.msg-text {
  max-width: 80%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.msg.user .msg-text {
  background: #1c1d1c;
  color: #fff;
  border-radius: 10px 2px 10px 10px;
}

.msg.assistant .msg-text {
  background: #f5f5f5;
  color: #333;
  border-radius: 2px 10px 10px 10px;
}

.typing { opacity: 0.6; }

.chat-footer {
  display: flex;
  gap: 6px;
  padding: 10px;
  border-top: 1px solid #f0f0f0;
}

.chat-footer input {
  flex: 1;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
}

.chat-footer input:focus { border-color: #2E7D32; }

.chat-footer button {
  width: 60px;
  background: #1c1d1c;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
}

.chat-footer button:disabled { opacity: 0.5; }

/* Markdown渲染样式 */
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) {
  font-size: 14px;
  font-weight: 700;
  margin: 8px 0 4px;
  color: #222;
}
.markdown-body :deep(ul), .markdown-body :deep(ol) {
  padding-left: 18px;
  margin: 4px 0;
}
.markdown-body :deep(li) { margin: 2px 0; }
.markdown-body :deep(strong) { color: #111; font-weight: 700; }
.markdown-body :deep(code) {
  background: #eee;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
}
.markdown-body :deep(pre) {
  background: #f5f5f5;
  padding: 8px 10px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 6px 0;
}
.markdown-body :deep(pre code) { background: transparent; padding: 0; }
.markdown-body :deep(p) { margin: 4px 0; }
.markdown-body :deep(blockquote) {
  border-left: 3px solid #2E7D32;
  padding-left: 10px;
  color: #666;
  margin: 6px 0;
}
</style>
