import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useAiChatStore } from './ai-chat.js'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem('loginUser') || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const user = ref(readUser())

  const token = computed(() => user.value?.token || '')
  const name = computed(() => user.value?.name || user.value?.username || '')
  const initial = computed(() => (name.value ? name.value.slice(0, 1) : 'T'))

  function setUser(value) {
    useAiChatStore().clear()
    user.value = value
    localStorage.setItem('loginUser', JSON.stringify(value))
  }

  function logout() {
    useAiChatStore().clear()
    user.value = null
    localStorage.removeItem('loginUser')
  }

  return { user, token, name, initial, setUser, logout }
})
