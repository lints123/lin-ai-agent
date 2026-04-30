<template>
  <div class="h-screen bg-void flex flex-col overflow-hidden" style="transition: background-color 0.3s ease;">
    <!-- Ambient gradient -->
    <div class="fixed inset-0 pointer-events-none" :style="ambientStyle" />

    <!-- Header -->
    <header class="relative z-10 flex items-center justify-between px-6 py-3 border-b" style="border-color: var(--border-subtle); transition: border-color 0.3s ease;">
      <!-- Left: Model selector -->
      <el-select v-model="selectedModel" placeholder="选择模型" size="small" style="width: 170px; height: 30px;">
        <el-option v-for="m in models" :key="m" :label="m" :value="m" />
      </el-select>

      <!-- Center: Title -->
      <h1 class="absolute left-1/2 -translate-x-1/2 font-display text-xl font-bold gradient-text tracking-tight whitespace-nowrap">
        Lin AI Agent
      </h1>

      <!-- Right: Theme toggle + Tabs -->
      <div class="flex items-center gap-3">
        <!-- Stream toggle -->
        <div v-show="activeTab === 'chat'" class="flex items-center gap-2">
          <span class="text-xs text-dim">流式</span>
          <el-switch v-model="useStream" />
        </div>

        <!-- RAG toggle -->
        <div v-show="activeTab === 'chat'" class="flex items-center gap-2">
          <span class="text-xs text-dim">RAG</span>
          <el-switch v-model="useRag" />
        </div>

        <button
          @click="toggleTheme"
          class="w-8 h-8 rounded-lg flex items-center justify-center transition-all duration-300 hover:opacity-80"
          :style="{ background: 'var(--color-surface)' }"
          :title="isDark ? '切换到亮色模式' : '切换到暗色模式'"
        >
          <svg v-if="isDark" class="w-4 h-4" style="color: var(--color-ash)" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <svg v-else class="w-4 h-4" style="color: var(--color-ash)" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
          </svg>
        </button>

        <nav class="flex bg-surface rounded-xl p-1">
          <button
            v-for="tab in tabs"
            :key="tab.name"
            @click="activeTab = tab.name"
            :class="[
              'px-5 py-1.5 rounded-lg text-sm font-medium transition-all duration-300 cursor-pointer',
              activeTab === tab.name
                ? 'bg-gradient-to-r from-accent to-coral text-on-accent shadow-lg'
                : 'text-ash hover:text-cream'
            ]"
          >
            {{ tab.label }}
          </button>
        </nav>
      </div>
    </header>

    <!-- Content -->
    <main class="relative z-10 flex-1 overflow-hidden">
      <ChatWindow v-show="activeTab === 'chat'" :model-id="selectedModel" :use-stream="useStream" :use-rag="useRag" />
      <AgentView v-show="activeTab === 'agent'" :model-id="selectedModel" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getModels } from './api/chat'
import ChatWindow from './components/ChatWindow.vue'
import AgentView from './components/AgentView.vue'

const activeTab = ref('chat')
const isDark = ref(true)
const models = ref<string[]>([])
const selectedModel = ref('')
const useStream = ref(true)
const useRag = ref(false)
const tabs = [
  { name: 'chat', label: 'Chat' },
  { name: 'agent', label: 'Agent' },
]

const ambientStyle = computed(() => ({
  background: `radial-gradient(ellipse at 15% 0%, var(--ambient-1) 0%, transparent 50%), radial-gradient(ellipse at 85% 100%, var(--ambient-2) 0%, transparent 50%)`,
}))

onMounted(async () => {
  // Theme
  const saved = localStorage.getItem('theme')
  if (saved) {
    isDark.value = saved === 'dark'
  } else {
    isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  applyTheme()

  // Models
  try {
    models.value = await getModels()
    if (models.value.length > 0) {
      selectedModel.value = models.value[0]
    }
  } catch {
    ElMessage.error('获取模型列表失败')
  }
})

function toggleTheme() {
  isDark.value = !isDark.value
  applyTheme()
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

function applyTheme() {
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
}
</script>
