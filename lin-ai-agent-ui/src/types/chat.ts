/** 对话请求参数 */
export interface ChatParam {
  message: string
  conversationId?: string
  modelId?: string
  useRag?: boolean
}

/** 对话响应结果 */
export interface ChatReply {
  content: string
  conversationId: string
  model: string
}

/** 后端统一响应 */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 聊天消息 */
export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

/** Agent 请求参数 */
export interface AgentParam {
  task: string
  modelId?: string
  workDir?: string
}

/** 工具调用轨迹 */
export interface ToolCallTrace {
  round: number
  toolName: string
  arguments: string
  result: string
}

/** Agent 响应结果 */
export interface AgentReply {
  result: string
  rounds: number
  traces: ToolCallTrace[]
}

/** 文档上传结果 */
export interface DocumentUploadResult {
  fileName: string
  chunkCount: number
  status: string
}
