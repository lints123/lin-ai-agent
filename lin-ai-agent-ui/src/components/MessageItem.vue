<template>
  <div :class="[
    'flex gap-3 mb-5',
    message.role === 'user' ? 'flex-row-reverse' : ''
  ]">
    <!-- Avatar -->
    <div :class="[
      'w-8 h-8 rounded-xl flex items-center justify-center text-xs font-bold shrink-0',
      message.role === 'user'
        ? 'bg-gradient-to-br from-accent to-coral text-on-accent shadow-lg'
        : 'bg-elevated text-ash border border-solid'
    ]" :style="message.role === 'assistant' ? 'border-color: var(--glass-border)' : ''">
      {{ message.role === 'user' ? 'U' : 'AI' }}
    </div>
    <!-- Bubble -->
    <div :class="[
      'max-w-[70%] px-4 py-3 text-sm leading-relaxed break-words',
      message.role === 'user'
        ? 'text-on-accent rounded-2xl rounded-tr-sm'
        : 'glass rounded-2xl rounded-tl-sm text-cream'
    ]" :style="message.role === 'user' ? 'background: var(--gradient-accent); box-shadow: var(--shadow-bubble);' : ''">
      <div v-if="message.role === 'assistant'" class="prose prose-sm max-w-none" v-html="renderedContent"></div>
      <div v-else class="font-medium">{{ message.content }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import type { ChatMessage } from '../types/chat'

const props = defineProps<{ message: ChatMessage }>()

// marked v18: use extension API instead of deprecated setOptions({ highlight })
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

const renderedContent = computed(() => {
  const result = marked.parse(props.message.content)
  return typeof result === 'string' ? result : ''
})
</script>
