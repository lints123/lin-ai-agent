<template>
  <div class="flex flex-col h-full">
    <!-- Content area -->
    <div ref="resultAreaRef" class="flex-1 overflow-y-auto px-6 py-4">
      <!-- Empty state -->
      <div v-if="!reply && !loading" class="flex flex-col items-center justify-center h-full animate-fade-in">
        <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-accent to-coral flex items-center justify-center mb-4 animate-pulse-glow">
          <svg class="w-8 h-8 text-void" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
        </div>
        <p class="text-ash text-sm">输入任务描述，Agent 将自主调用工具完成</p>
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="flex flex-col items-center justify-center h-full animate-fade-in">
        <div class="relative mb-4">
          <div class="w-14 h-14 rounded-full border-2 border-t-accent animate-spin" style="border-color: var(--spinner-track); border-top-color: var(--spinner-accent);"></div>
          <div class="absolute inset-0 flex items-center justify-center">
            <div class="w-4 h-4 rounded-full bg-gradient-to-br from-accent to-coral"></div>
          </div>
        </div>
        <p class="text-cream text-base font-medium font-display">Agent 正在执行任务...</p>
        <p class="text-dim text-xs mt-2">模型自主规划并调用工具中，请耐心等待</p>
      </div>

      <!-- Results -->
      <template v-if="reply">
        <!-- Tool traces -->
        <div v-if="reply.traces.length > 0" class="mb-6 animate-fade-in-up">
          <h3 class="text-sm font-medium text-ash mb-4 font-display">
            工具调用轨迹（共 {{ reply.rounds }} 轮）
          </h3>
          <el-timeline>
            <el-timeline-item
              v-for="(trace, idx) in reply.traces"
              :key="idx"
              :timestamp="`Round ${trace.round}`"
              placement="top"
              type="primary"
            >
              <div class="glass rounded-xl p-4 glass-hover transition-all duration-300">
                <div class="mb-2">
                  <el-tag size="small" effect="plain" round>{{ trace.toolName }}</el-tag>
                </div>
                <div class="text-xs text-ash mt-1">
                  <span class="font-medium text-cream">参数:</span>
                  <code class="bg-void px-2 py-0.5 rounded text-accent ml-1 break-all" style="font-size: 11px;">{{ trace.arguments }}</code>
                </div>
                <div class="text-xs text-ash mt-2">
                  <span class="font-medium text-cream">结果:</span>
                  <span class="ml-1" style="color: var(--text-result);">{{ trace.result }}</span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <!-- Final result -->
        <div class="mt-6 animate-fade-in-up">
          <h3 class="text-sm font-medium text-ash mb-3 font-display">最终结果</h3>
          <div class="glass rounded-xl p-5">
            <div class="prose prose-sm max-w-none" v-html="renderedResult"></div>
          </div>
        </div>
      </template>
    </div>

    <!-- Input area -->
    <div class="px-6 py-4 border-t" style="border-color: var(--border-subtle)">
      <div class="flex items-end gap-3">
        <el-input
          v-model="taskInput"
          type="textarea"
          :rows="2"
          placeholder="描述任务，例如：搜索 Spring AI 最新版本并生成 PDF 报告"
          resize="none"
          class="flex-1"
          @keydown.enter.exact.prevent="executeTask"
        />
        <el-button
          type="primary"
          :disabled="loading || !taskInput.trim()"
          :loading="loading"
          @click="executeTask"
          style="height: 40px; padding: 0 24px;"
        >
          执行
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { executeAgent } from '../api/chat'
import type { AgentReply } from '../types/chat'

marked.use({
  renderer: {
    code(token: any) {
      const text = typeof token === 'object' ? token.text : token
      const lang = typeof token === 'object' ? token.lang : undefined
      if (lang && hljs.getLanguage(lang)) {
        return `<pre><code class="hljs">${hljs.highlight(text, { language: lang }).value}</code></pre>`
      }
      return `<pre><code class="hljs">${hljs.highlightAuto(text).value}</code></pre>`
    }
  }
})

const props = defineProps<{ modelId: string }>()

const taskInput = ref('')
const loading = ref(false)
const reply = ref<AgentReply | null>(null)
const resultAreaRef = ref<HTMLElement>()

const renderedResult = computed(() => {
  if (!reply.value) return ''
  const result = marked.parse(reply.value.result)
  return typeof result === 'string' ? result : ''
})

async function executeTask() {
  const task = taskInput.value.trim()
  if (!task || loading.value) return

  reply.value = null
  loading.value = true

  try {
    reply.value = await executeAgent({
      task,
      modelId: props.modelId || undefined,
    })
    nextTick(() => {
      if (resultAreaRef.value) {
        resultAreaRef.value.scrollTop = resultAreaRef.value.scrollHeight
      }
    })
  } catch (e: any) {
    ElMessage.error(e.message || 'Agent 执行失败')
  } finally {
    loading.value = false
  }
}
</script>
