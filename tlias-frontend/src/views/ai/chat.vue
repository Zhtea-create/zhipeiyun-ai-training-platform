<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useAiChatStore } from '@/stores/ai-chat'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Delete, MagicStick, Promotion } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { aiChatApi, getAiStatusApi } from '@/api/ai'
import { renderMarkdown } from '@/utils/markdown'

const suggestions = [
  '现在一共有多少学员？',
  '哪个班的人数最多？',
  '学员的学历分布是怎样的？',
  '员工职位和性别比例如何？',
  '最近有哪些操作记录？',
]

const chatStore = useAiChatStore()
const { messages, input, sending, mode } = storeToRefs(chatStore)
const messagesEl = ref(null)

const modeLabel = computed(() => {
  if (mode.value === 'live') return '真实模型'
  if (mode.value === 'mock') return '演示模式'
  return '未连接'
})

const modeType = computed(() => (mode.value === 'live' ? 'success' : mode.value === 'mock' ? 'warning' : 'info'))

const scrollToBottom = async () => {
  await nextTick()
  const el = messagesEl.value
  if (el) el.scrollTop = el.scrollHeight
}

const useSuggestion = (text) => {
  input.value = text
  send()
}

const send = async () => {
  await chatStore.sendMessage(aiChatApi)
}

const clearChat = () => {
  chatStore.clear()
  chatStore.loadStatus(getAiStatusApi)
  ElMessage.success('已清空对话')
}

watch(() => messages.value.length, scrollToBottom)
onMounted(() => {
  chatStore.loadStatus(getAiStatusApi)
  scrollToBottom()
})
</script>

<template>
  <div class="page ai-page">
    <PageHeader title="AI 助手" subtitle="用自然语言提问，AI 按需查询班级、学员与员工数据">
      <template #actions>
        <el-tag :type="modeType" effect="light" round>
          <span class="mode-dot"></span>
          {{ modeLabel }}
        </el-tag>
        <el-button :icon="Delete" @click="clearChat">清空对话</el-button>
      </template>
    </PageHeader>

    <div class="ai-layout">
      <div class="chat-card card">
        <div ref="messagesEl" class="messages">
          <div v-if="!messages.length" class="welcome">
            <div class="welcome-orb">
              <el-icon :size="30"><MagicStick /></el-icon>
            </div>
            <h2>你好，我是智培云 AI</h2>
            <p>我可以读取系统中的班级、学员、员工与日志数据，帮你快速找到答案。</p>
            <div class="welcome-chips">
              <button v-for="q in suggestions.slice(0, 3)" :key="q" @click="useSuggestion(q)">
                {{ q }}
              </button>
            </div>
          </div>

          <div v-for="msg in messages" :key="msg.id" class="msg" :class="msg.role">
            <div class="msg-avatar">
              <el-icon v-if="msg.role === 'assistant'"><MagicStick /></el-icon>
              <span v-else>我</span>
            </div>
            <div class="bubble">
              <div v-if="msg.role === 'assistant'" class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
              <div v-else class="user-text">{{ msg.content }}</div>
            </div>
          </div>

          <div v-if="sending" class="msg assistant">
            <div class="msg-avatar"><el-icon><MagicStick /></el-icon></div>
            <div class="bubble typing">
              <i></i><i></i><i></i>
            </div>
          </div>
        </div>

        <div class="composer">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入你的问题，例如：哪个班人数最多？"
            resize="none"
            @keydown.enter.exact.prevent="send"
          />
          <el-button
            class="send-btn"
            type="primary"
            :icon="Promotion"
            :loading="sending"
            :disabled="!input.trim()"
            @click="send"
          >
            发送
          </el-button>
        </div>
        <div class="composer-tip">
          <el-icon><ChatDotRound /></el-icon>
          AI 可能会出错，请核对重要数据 · 当前模式：{{ modeLabel }}
        </div>
      </div>

      <aside class="side-card card">
        <div class="side-head">
          <h3>试试这样问</h3>
          <span>点击直接发送</span>
        </div>
        <button v-for="q in suggestions" :key="q" class="suggestion" @click="useSuggestion(q)">
          <span class="q-mark">Q</span>
          {{ q }}
        </button>

        <div class="side-note">
          <el-icon><MagicStick /></el-icon>
          <p>AI 使用只读工具查询统计及必要摘要，不获取密码、身份证号、手机号，不修改业务数据。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.ai-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 18px;
  flex: 1;
  min-height: 0;
}

.chat-card {
  display: flex;
  flex-direction: column;
  min-height: 560px;
  overflow: hidden;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 26px 28px;
  background:
    radial-gradient(80% 60% at 80% 0%, rgba(224, 231, 255, 0.45), transparent 60%),
    linear-gradient(180deg, #fbfcff, #ffffff);
}

.welcome {
  max-width: 520px;
  margin: 40px auto 0;
  text-align: center;
}

.welcome-orb {
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  margin: 0 auto 18px;
  border-radius: 24px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6 55%, #06b6d4);
  color: #fff;
  box-shadow: 0 16px 36px rgba(99, 102, 241, 0.35);
}

.welcome h2 {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--ink-900);
}

.welcome p {
  margin-top: 8px;
  color: var(--ink-500);
  font-size: 13px;
  line-height: 1.7;
}

.welcome-chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 22px;
}

.welcome-chips button {
  padding: 8px 13px;
  border-radius: 999px;
  border: 1px solid #e0e7ff;
  background: #fff;
  color: #4f46e5;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.welcome-chips button:hover {
  background: #eef2ff;
  transform: translateY(-1px);
}

.msg {
  display: flex;
  gap: 12px;
  margin-bottom: 22px;
}

.msg.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 11px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
}

.assistant .msg-avatar {
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  color: #fff;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.28);
}

.user .msg-avatar {
  background: #e2e8f0;
  color: var(--ink-600, #475569);
}

.bubble {
  max-width: min(680px, 78%);
  padding: 13px 16px;
  border-radius: 16px;
  font-size: 13.5px;
  line-height: 1.75;
}

.assistant .bubble {
  background: #fff;
  border: 1px solid #eef2f7;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
  border-top-left-radius: 5px;
  color: var(--ink-700);
}

.user .bubble {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  border-top-right-radius: 5px;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.22);
}

.user-text {
  white-space: pre-wrap;
}

.typing {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 16px 18px;
}

.typing i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #a5b4fc;
  animation: blink 1.2s infinite ease-in-out;
}

.typing i:nth-child(2) {
  animation-delay: 0.18s;
}
.typing i:nth-child(3) {
  animation-delay: 0.36s;
}

@keyframes blink {
  0%,
  80%,
  100% {
    transform: scale(0.7);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 18px 8px;
  border-top: 1px solid var(--line);
  background: #fff;
}

.composer :deep(.el-textarea__inner) {
  padding: 11px 14px;
  border-radius: 13px;
  background: #f8fafc;
  box-shadow: none;
  border: 1px solid #eef2f7;
}

.composer :deep(.el-textarea__inner:focus) {
  border-color: #c7d2fe;
  background: #fff;
}

.send-btn {
  height: 42px;
  padding: 0 20px;
  border-radius: 12px;
}

.composer-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 20px 12px;
  color: var(--ink-400);
  font-size: 10.5px;
}

.side-card {
  padding: 22px 20px;
  height: fit-content;
}

.side-head {
  margin-bottom: 14px;
}

.side-head h3 {
  font-size: 14.5px;
  color: var(--ink-900);
}

.side-head span {
  display: block;
  margin-top: 3px;
  color: var(--ink-400);
  font-size: 11px;
}

.suggestion {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  width: 100%;
  margin-bottom: 8px;
  padding: 11px 12px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fbfcff;
  color: var(--ink-600, #475569);
  font-size: 12px;
  text-align: left;
  line-height: 1.5;
  cursor: pointer;
  transition: all 0.18s ease;
}

.suggestion:hover {
  border-color: #c7d2fe;
  background: #eef2ff;
  color: #4338ca;
  transform: translateX(2px);
}

.q-mark {
  display: grid;
  place-items: center;
  width: 19px;
  height: 19px;
  border-radius: 6px;
  background: #eef2ff;
  color: #6366f1;
  font-size: 10px;
  font-weight: 800;
  flex-shrink: 0;
}

.side-note {
  display: flex;
  gap: 8px;
  margin-top: 18px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  color: var(--ink-400);
}

.side-note p {
  font-size: 10.5px;
  line-height: 1.6;
}

.mode-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  border-radius: 50%;
  background: currentColor;
  vertical-align: middle;
}

@media (max-width: 1100px) {
  .ai-layout {
    grid-template-columns: 1fr;
  }
  .side-card {
    display: none;
  }
}
</style>
