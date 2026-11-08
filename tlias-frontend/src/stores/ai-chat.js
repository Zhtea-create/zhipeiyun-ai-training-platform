import { ref, watch } from 'vue'
import { defineStore } from 'pinia'

const CACHE_KEY = 'zhipeiyun.ai-chat.v1'

function currentOwner() {
  try {
    const user = JSON.parse(localStorage.getItem('loginUser') || 'null')
    if (!user?.token) return null
    return user.id != null ? `id:${user.id}` : user.username ? `username:${user.username}` : null
  } catch {
    return null
  }
}

function removeCache() {
  try { sessionStorage.removeItem(CACHE_KEY) } catch { /* Storage can be disabled. */ }
}

function restore() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(CACHE_KEY) || 'null')
    const owner = currentOwner()
    if (!owner || saved?.owner !== owner || !Array.isArray(saved.messages)) {
      removeCache()
      return null
    }
    if (saved.messages.length > 200 || saved.messages.some((message) =>
      !message || !['user', 'assistant'].includes(message.role) || typeof message.content !== 'string')) {
      removeCache()
      return null
    }
    return {
      messages: saved.messages.map(({ role, content }, index) => ({ id: index + 1, role, content })),
      input: typeof saved.input === 'string' ? saved.input : '',
      mode: ['live', 'mock', 'offline'].includes(saved.mode) ? saved.mode : 'offline',
    }
  } catch {
    removeCache()
    return null
  }
}

// Pinia survives route changes; tab-scoped storage also survives full page reloads.
export const useAiChatStore = defineStore('ai-chat', () => {
  const saved = restore()
  const messages = ref(saved?.messages || [])
  const input = ref(saved?.input || '')
  const sending = ref(false)
  const mode = ref(saved?.mode || 'offline')
  let sequence = messages.value.length
  let generation = 0

  watch([messages, input, mode], () => {
    try {
      const owner = currentOwner()
      if (!owner) { removeCache(); return }
      sessionStorage.setItem(CACHE_KEY, JSON.stringify({
        owner,
        messages: messages.value.slice(-200).map(({ role, content }) => ({ role, content })),
        input: input.value,
        mode: mode.value,
      }))
    } catch {
      // Do not restore stale history if the latest cache write fails (e.g. quota exceeded).
      removeCache()
    }
  }, { deep: true, flush: 'sync' })

  function clear() {
    generation++
    messages.value = []
    input.value = ''
    sending.value = false
    mode.value = 'offline'
    removeCache()
  }

  async function loadStatus(fetchStatus) {
    const current = generation
    const messageSequence = sequence
    try {
      const res = await fetchStatus()
      if (current === generation && messageSequence === sequence && !sending.value) {
        mode.value = res.data?.mode || 'mock'
      }
    } catch {
      if (current === generation && messageSequence === sequence && !sending.value) mode.value = 'offline'
    }
  }

  async function sendMessage(sendRequest) {
    const text = input.value.trim()
    if (!text || sending.value) return
    const current = generation
    const history = messages.value.map(({ role, content }) => ({ role, content }))
    messages.value.push({ id: ++sequence, role: 'user', content: text })
    input.value = ''
    sending.value = true
    try {
      const res = await sendRequest({ message: text, history })
      if (current !== generation) return
      if (res.data?.mode) mode.value = res.data.mode
      messages.value.push({
        id: ++sequence,
        role: 'assistant',
        content: res.data?.answer || '我暂时没有生成有效回答，请换个问法试试。',
      })
    } catch {
      if (current !== generation) return
      messages.value.push({
        id: ++sequence,
        role: 'assistant',
        content: 'AI 服务暂时不可用，请稍后重试。',
      })
    } finally {
      // A cleared conversation may already have a different request in progress.
      if (current === generation) sending.value = false
    }
  }

  return { messages, input, sending, mode, clear, loadStatus, sendMessage }
})
