import axios from 'axios'
import type { ChatParam, ChatReply, Result, AgentParam, AgentReply, DocumentUploadResult } from '../types/chat'

/** 获取可用模型列表 */
export async function getModels(): Promise<string[]> {
  const { data } = await axios.get<Result<string[]>>('/api/chat/models')
  return data.data
}

/** 同步对话 */
export async function chat(param: ChatParam): Promise<ChatReply> {
  const { data } = await axios.post<Result<ChatReply>>('/api/chat', param)
  return data.data
}

/** 流式对话（SSE） */
export async function streamChat(
  param: ChatParam,
  onChunk: (text: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(param),
    signal,
  })

  if (!response.ok) {
    throw new Error(`HTTP error: ${response.status}`)
  }

  const reader = response.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // 解析 SSE 格式：每条消息以 \n\n 分隔，格式为 "data:内容\n"
    const parts = buffer.split('\n\n')
    buffer = parts.pop()! // 最后一段可能不完整，保留到下次

    for (const part of parts) {
      for (const line of part.split('\n')) {
        if (line.startsWith('data:')) {
          const text = line.slice(5)
          if (text) {
            onChunk(text)
          }
        }
      }
    }
  }
}

/** Agent 任务执行 */
export async function executeAgent(param: AgentParam): Promise<AgentReply> {
  const { data } = await axios.post<Result<AgentReply>>('/api/chat/agent', param)
  return data.data
}

/** 上传文档 */
export async function uploadDocument(file: File): Promise<DocumentUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await axios.post<Result<DocumentUploadResult>>(
    '/api/documents/upload',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
  return data.data
}
