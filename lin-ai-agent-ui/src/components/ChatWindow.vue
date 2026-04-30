<template>
  <div class="flex flex-col h-full">
    <!-- Message list (centered column) -->
    <div ref="messageListRef" class="flex-1 overflow-y-auto">
      <div class="max-w-3xl mx-auto px-6 py-4">
        <!-- Empty state -->
        <div v-if="messages.length === 0 && !loading" class="flex flex-col items-center justify-center animate-fade-in" style="min-height: 60vh;">
          <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-accent to-coral flex items-center justify-center mb-4 animate-pulse-glow">
            <svg class="w-8 h-8 text-on-accent" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
            </svg>
          </div>
          <p class="text-ash text-sm">发送一条消息开始对话吧</p>
        </div>

        <MessageItem v-for="(msg, index) in messages" :key="index" :message="msg" class="animate-fade-in-up" :style="{ animationDelay: `${index * 0.03}s` }" />
      </div>
    </div>

    <!-- Input area (centered capsule) -->
    <div class="px-4 pb-5 pt-2">
      <div class="max-w-3xl mx-auto">
        <div class="glass rounded-2xl flex items-end p-2 pl-4 gap-2">
          <!-- Upload button -->
          <button
            @click="fileInputRef?.click()"
            :disabled="uploading"
            class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0 transition-opacity hover:opacity-70"
            :class="uploading ? 'opacity-50 cursor-wait' : 'cursor-pointer'"
            title="上传文档 (PDF/TXT)"
          >
            <svg class="w-4 h-4" style="color: var(--color-ash)" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M18.364 5.636l-3.536-3.536a2 2 0 00-2.828 0L4.929 9.171a2 2 0 000 2.828l7.07 7.071a2 2 0 002.83 0l3.535-3.536M14.12 2.12l7.07 7.07M3 21l3.536-3.536" />
            </svg>
          </button>
          <input ref="fileInputRef" type="file" accept=".pdf,.txt" class="hidden" @change="handleFileUpload" />
          <textarea
            ref="textareaRef"
            v-model="inputText"
            placeholder="给 Lin AI Agent 发送消息..."
            :disabled="loading"
            rows="1"
            class="chat-textarea flex-1 bg-transparent border-none outline-none resize-none text-sm leading-relaxed py-1"
            @input="autoResize"
            @keydown.enter.exact.prevent="handleAction"
          />
          <!-- Send / Stop button -->
          <button
            @click="handleAction"
            :disabled="!loading && !inputText.trim()"
            class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 transition-all duration-300"
            :class="loading
              ? 'bg-elevated cursor-pointer hover:opacity-80'
              : inputText.trim() ? 'bg-gradient-to-r from-accent to-coral cursor-pointer' : 'bg-elevated opacity-40'"
            :title="loading ? '停止生成' : '发送'"
          >
            <!-- Stop icon (square) when loading -->
            <svg v-if="loading" class="w-3.5 h-3.5" style="color: var(--color-ash)" viewBox="0 0 24 24" fill="currentColor">
              <rect x="6" y="6" width="12" height="12" rx="2" />
            </svg>
            <!-- Paper airplane when idle -->
            <svg v-else class="w-3.5 h-3.5 text-on-accent" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { chat, streamChat, uploadDocument } from '../api/chat'
import MessageItem from './MessageItem.vue'
import type { ChatMessage } from '../types/chat'

const props = defineProps<{ modelId: string; useStream: boolean; useRag: boolean }>()

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const loading = ref(false)
const uploading = ref(false)
const conversationId = ref<string | undefined>(undefined)
const messageListRef = ref<HTMLElement>()
const textareaRef = ref<HTMLTextAreaElement>()
const fileInputRef = ref<HTMLInputElement>()
const abortController = ref<AbortController | null>(null)

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

function handleAction() {
  if (loading.value) {
    stopGeneration()
  } else {
    sendMessage()
  }
}

async function handleFileUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // reset so same file can be re-selected
  if (!file) return

  const ext = file.name.toLowerCase()
  if (!ext.endsWith('.pdf') && !ext.endsWith('.txt')) {
    ElMessage.warning('仅支持 PDF 和 TXT 文件')
    return
  }

  uploading.value = true
  try {
    const result = await uploadDocument(file)
    ElMessage.success(`上传成功: ${result.fileName}，已分割为 ${result.chunkCount} 个片段`)
    messages.value.push({
      role: 'assistant',
      content: `📎 文档「${result.fileName}」已上传并索引（${result.chunkCount} 个片段），现在可以基于该文档提问了。`,
    })
    scrollToBottom()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function stopGeneration() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  loading.value = false
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  if (textareaRef.value) textareaRef.value.style.height = 'auto'
  loading.value = true
  scrollToBottom()

  const param = {
    message: text,
    conversationId: conversationId.value,
    modelId: props.modelId || undefined,
    useRag: props.useRag || undefined,
  }

  try {
    if (props.useStream) {
      messages.value.push({ role: 'assistant', content: '' })
      const lastIndex = messages.value.length - 1
      scrollToBottom()

      abortController.value = new AbortController()
      await streamChat(param, (chunk) => {
        messages.value[lastIndex].content += chunk
        scrollToBottom()
      }, abortController.value.signal)
    } else {
      abortController.value = new AbortController()
      const reply = await chat(param)
      conversationId.value = reply.conversationId
      messages.value.push({ role: 'assistant', content: reply.content })
    }
  } catch (e: any) {
    if (e.name === 'AbortError') {
      return
    }
    ElMessage.error(e.message || '请求失败')
  } finally {
    loading.value = false
    abortController.value = null
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-textarea {
  color: var(--input-text);
  max-height: 120px;
  font-family: "DM Sans", ui-sans-serif, system-ui, sans-serif;
}
.chat-textarea::placeholder {
  color: var(--input-placeholder);
}
.chat-textarea:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
