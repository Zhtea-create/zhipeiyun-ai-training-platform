import test from 'node:test'
import assert from 'node:assert/strict'
import { createPinia, setActivePinia } from 'pinia'
import { useAiChatStore } from '../src/stores/ai-chat.js'
import { useUserStore } from '../src/stores/user.js'

function setup() {
  setActivePinia(createPinia())
  const storage = new Map()
  globalThis.localStorage = {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value),
    removeItem: (key) => storage.delete(key),
  }
  const session = new Map()
  globalThis.sessionStorage = {
    getItem: (key) => session.get(key) ?? null,
    setItem: (key, value) => session.set(key, value),
    removeItem: (key) => session.delete(key),
  }
  return useAiChatStore()
}

function deferred() {
  let resolve
  let reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('a new route consumer retains draft and pending reply, and sends conversation history', async () => {
  const chat = setup()
  chat.input = '第一个问题'
  const reply = deferred()
  const pending = chat.sendMessage(() => reply.promise)
  const returnedPage = useAiChatStore()
  assert.equal(returnedPage.sending, true)
  returnedPage.input = '后续草稿'
  reply.resolve({ data: { answer: '第一条回答', mode: 'live' } })
  await pending
  assert.equal(returnedPage.messages.length, 2)
  assert.equal(returnedPage.messages[1].content, '第一条回答')
  assert.equal(returnedPage.input, '后续草稿')
  await returnedPage.sendMessage(async (body) => {
    assert.equal(body.history.length, 2)
    assert.equal(body.message, '后续草稿')
    return { data: { answer: '第二条回答' } }
  })
  assert.equal(chat.messages.length, 4)
})

test('clear discards old replies without ending a newer pending request', async () => {
  const chat = setup()
  const oldReply = deferred()
  chat.input = '旧问题'
  const oldRequest = chat.sendMessage(() => oldReply.promise)
  chat.clear()
  const newReply = deferred()
  chat.input = '新问题'
  const newRequest = chat.sendMessage(() => newReply.promise)
  oldReply.resolve({ data: { answer: '旧回答', mode: 'live' } })
  await oldRequest
  assert.equal(chat.sending, true)
  assert.equal(chat.messages.length, 1)
  newReply.resolve({ data: { answer: '新回答', mode: 'mock' } })
  await newRequest
  assert.equal(chat.messages[1].content, '新回答')
  assert.equal(chat.mode, 'mock')
})

test('logout and account login clear conversation and ignore delayed failures', async () => {
  const chat = setup()
  const user = useUserStore()
  user.setUser({ username: 'first', token: 'test' })
  chat.input = '旧账号问题'
  const reply = deferred()
  const pending = chat.sendMessage(() => reply.promise)
  user.logout()
  user.setUser({ username: 'second', token: 'test2' })
  reply.reject(new Error('late failure'))
  await pending
  assert.deepEqual(chat.messages, [])
  assert.equal(chat.input, '')
  assert.equal(chat.sending, false)
})

test('repeated sends are ignored while pending; failed request permits retry', async () => {
  const chat = setup()
  const reply = deferred()
  chat.input = '问题'
  const pending = chat.sendMessage(() => reply.promise)
  chat.input = '草稿'
  await chat.sendMessage(() => assert.fail('duplicate request'))
  reply.reject(new Error('offline'))
  await pending
  assert.equal(chat.sending, false)
  assert.equal(chat.messages[1].role, 'assistant')
  assert.equal(chat.input, '草稿')
})

test('a delayed status check cannot overwrite the actual reply mode', async () => {
  const chat = setup()
  const status = deferred()
  const pendingStatus = chat.loadStatus(() => status.promise)
  chat.input = '问题'
  await chat.sendMessage(async () => ({ data: { answer: '降级回答', mode: 'mock' } }))
  status.resolve({ data: { mode: 'live' } })
  await pendingStatus
  assert.equal(chat.mode, 'mock')
})

test('a full app reload restores messages and draft and includes history in the next request', async () => {
  const chat = setup()
  localStorage.setItem('loginUser', JSON.stringify({ id: 1, token: 'token' }))
  chat.input = '持久化问题'
  await chat.sendMessage(async () => ({ data: { answer: '持久化回答', mode: 'live' } }))
  chat.input = '未发送草稿'
  setActivePinia(createPinia()) // A new app, not just another consumer of the same store.
  const restored = useAiChatStore()
  assert.equal(restored.messages.length, 2)
  assert.equal(restored.messages[1].content, '持久化回答')
  assert.equal(restored.input, '未发送草稿')
  assert.equal(restored.mode, 'live')
  assert.equal(restored.sending, false)
  await restored.sendMessage(async ({ history }) => {
    assert.equal(history.length, 2)
    return { data: { answer: '追问回答' } }
  })
  assert.equal(new Set(restored.messages.map(({ id }) => id)).size, 4)
})

test('clear and logout remove persisted history, and another account cannot restore it', async () => {
  const chat = setup()
  const user = useUserStore()
  user.setUser({ id: 1, token: 'one' })
  chat.input = '账号一的问题'
  await chat.sendMessage(async () => ({ data: { answer: '账号一的回答' } }))
  assert.ok(sessionStorage.getItem('zhipeiyun.ai-chat.v1'))
  localStorage.setItem('loginUser', JSON.stringify({ id: 2, token: 'two' }))
  setActivePinia(createPinia())
  const other = useAiChatStore()
  assert.deepEqual(other.messages, [])
  assert.equal(sessionStorage.getItem('zhipeiyun.ai-chat.v1'), null)
  other.input = '另一个问题'
  await other.sendMessage(async () => ({ data: { answer: '回答' } }))
  other.clear()
  assert.equal(sessionStorage.getItem('zhipeiyun.ai-chat.v1'), null)
  setActivePinia(createPinia())
  assert.deepEqual(useAiChatStore().messages, [])
  useAiChatStore().input = '退出前草稿'
  useUserStore().logout()
  assert.equal(sessionStorage.getItem('zhipeiyun.ai-chat.v1'), null)
})

test('reloading during a pending response restores the question without a permanent sending state', async () => {
  const chat = setup()
  localStorage.setItem('loginUser', JSON.stringify({ username: 'test', token: 'token' }))
  chat.input = '等待回答的问题'
  const reply = deferred()
  const pending = chat.sendMessage(() => reply.promise)
  setActivePinia(createPinia())
  const restored = useAiChatStore()
  assert.equal(restored.messages[0].content, '等待回答的问题')
  assert.equal(restored.sending, false)
  reply.resolve({ data: { answer: '完成' } })
  await pending
})

test('invalid cache and unavailable storage do not break chatting', async () => {
  setup()
  localStorage.setItem('loginUser', JSON.stringify({ id: 1, token: 'token' }))
  sessionStorage.setItem('zhipeiyun.ai-chat.v1', '{invalid')
  setActivePinia(createPinia())
  assert.deepEqual(useAiChatStore().messages, [])
  sessionStorage.setItem = () => { throw new Error('storage unavailable') }
  const chat = useAiChatStore()
  chat.input = '问题'
  await chat.sendMessage(async () => ({ data: { answer: '回答' } }))
  assert.equal(chat.messages.length, 2)
})
