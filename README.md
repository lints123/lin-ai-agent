# Lin AI Agent - 学习指南

> 一个基于 Spring Boot 3.4 + Spring AI Alibaba 的 AI 智能体项目，从零到一掌握 AI 应用开发。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.4.4 |
| AI 框架 | Spring AI Alibaba | 1.1.2.2 |
| 大模型 | 通义千问 (Qwen) | qwen-plus |
| 数据库 | MySQL + Spring Data JPA | - |
| 向量存储 | SimpleVectorStore | - |
| 工具库 | Hutool | 5.8.34 |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| 前端 | Vue 3 + TypeScript + Element Plus | - |

## 项目结构

```
src/main/java/com/lin/ai/agent/
├── common/          # 通用类（Result、AiConstants）
├── config/          # 配置类（ChatModelConfig、RagConfig）
├── controller/      # REST 控制器
├── advisor/         # AI 拦截器（日志、ReReading）
├── model/
│   ├── dto/         # 请求对象（ChatParam、AgentParam）
│   └── vo/          # 响应对象（ChatReply、AgentReply）
├── service/         # 服务接口
│   └── impl/        # 服务实现
└── tool/            # Agent 工具集
    ├── BaiduSearchTool.java       # 百度搜索
    ├── WebScraperTool.java        # 网页抓取
    ├── FileReadTool.java          # 文件读取
    ├── FileWriteTool.java         # 文件写入
    ├── ResourceDownloadTool.java  # 资源下载
    ├── TerminalTool.java          # 终端命令
    ├── PdfGenerationTool.java     # PDF 生成
    ├── ResearchTool.java          # 组合调研（搜索+抓取+汇总）
    ├── TerminationTool.java       # 终止信号
    └── ToolValidator.java         # 工具输入校验
```

---

## 课程一：同步对话

**学习目标**：理解 Spring AI 如何与通义千问对接，实现一问一答的对话。

### 核心概念

```
用户消息 → ChatClient → 通义千问 API → 返回回复
```

### 关键代码

**1. 模型注册** — `config/ChatModelConfig.java`

Spring AI Alibaba 提供了 `dashscopeChatModel` Bean，通过 `ChatClient.builder()` 封装使用：

```java
ChatClient qwenClient = ChatClient.builder(dashScopeChatModel)
        .defaultAdvisors(reReadingAdvisor, loggingAdvisor)
        .build();
```

**2. 对话服务** — `service/impl/ChatServiceImpl.java`

```java
// 一次同步调用
ChatReply reply = chatClient.prompt()
        .user(message)
        .call()
        .content();
```

**3. 接口定义** — `controller/ChatController.java`

```java
@PostMapping
public Result<ChatReply> chat(@RequestBody ChatParam param) {
    return Result.success(chatService.chat(param));
}
```

### 学习要点

- `ChatClient` 是 Spring AI 的核心入口，封装了模型调用逻辑
- `system()` 设置系统提示词，`user()` 设置用户消息
- `@Schema` 注解为每个 DTO 字段添加 API 文档说明

### 动手练习

1. 启动项目，访问 `http://localhost:8080/doc.html`
2. 调用 `POST /api/chat`，body: `{"message": "你好"}`
3. 观察返回的 `ChatReply` 结构
4. 尝试修改 `AiConstants.SYSTEM_PROMPT`，改变 AI 的角色

---

## 课程二：流式对话（SSE）

**学习目标**：理解 Server-Sent Events 实现逐字输出，提升用户体验。

### 核心概念

```
用户消息 → ChatClient.stream() → Flux<String> → SSE 逐字推送
```

同步对话用户要等全部生成完才能看到，流式对话则逐字实时返回。

### 关键代码

**1. 流式调用** — `service/impl/ChatServiceImpl.java`

```java
public Flux<String> streamChat(ChatParam param) {
    return chatClient.prompt()
            .user(message)
            .stream()
            .content();
}
```

**2. SSE 端点** — `controller/ChatController.java`

```java
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestBody ChatParam param) {
    return chatService.streamChat(param);
}
```

`TEXT_EVENT_STREAM_VALUE` = `"text/event-stream"`，告诉浏览器这是一个 SSE 流。

### 学习要点

- `stream()` 返回 `Flux<String>`（响应式流），`call()` 返回 String（阻塞）
- SSE 是单向推送：服务端 → 客户端，不需要 WebSocket 的双向通信
- 前端用 `EventSource` 或 `fetch` 的 `ReadableStream` 接收

### 动手练习

1. 调用 `POST /api/chat/stream`，观察响应是否逐字返回
2. 在前端 `ChatWindow.vue` 中找到流式消息的渲染逻辑
3. 对比同步和流式的用户体验差异

---

## 课程三：会话记忆

**学习目标**：理解多轮对话的上下文管理机制。

### 核心概念

```
Round 1: [用户: 你好] → [AI: 你好，有什么可以帮你？]
Round 2: [用户: 我叫小明] → [AI: 好的小明...]
Round 3: [用户: 我叫什么？] → [AI: 你叫小明]  ← 记住了之前的对话
```

LLM 本身没有记忆，每次调用都是无状态的。会话记忆 = 把历史消息拼接到请求中。

### 关键代码

**`service/impl/ChatServiceImpl.java`**

```java
// 按会话 ID 存储历史消息
Map<String, List<Message>> history = chatMemoryService.getHistory(conversationId);

// 拼接历史 + 当前消息，一起发给模型
List<Message> allMessages = new ArrayList<>(history);
allMessages.add(new UserMessage(currentMessage));

// 调用完成后，保存本轮对话
history.add(new UserMessage(currentMessage));
history.add(new AssistantMessage(reply));
```

### 学习要点

- 会话记忆本质上是一个 `Map<sessionId, List<Message>>`
- 历史消息越多，token 消耗越大，需要限制最大条数（项目中设为 20 条）
- 这是内存级记忆，应用重启后丢失。持久化需要用数据库或 Redis

### 动手练习

1. 连续发送多条消息，验证 AI 能记住之前的对话
2. 用不同的 `conversationId` 调用，验证会话之间互不干扰
3. 思考：如果对话超过 20 条，应该如何处理？（提示：淘汰最早的、或摘要压缩）

---

## 课程四：RAG（检索增强生成）

**学习目标**：理解如何让 AI 基于私有知识库回答问题。

### 核心概念

```
用户问题
  ↓
1. 向量化用户问题（Embedding）
2. 在向量数据库中检索相似文档
3. 将检索到的文档作为上下文注入 Prompt
4. LLM 基于上下文生成回答
```

没有 RAG：AI 只知道训练时的知识（截止日期之前）
有 RAG：AI 可以回答你的私有文档中的内容

### 关键代码

**1. 文档入库** — `config/RagConfig.java`

启动时自动加载 `docs/` 目录下的文档，切割为向量并存储：

```java
// 读取文档 → 切割为段落 → 生成向量嵌入 → 存入 VectorStore
vectorStore.add(chunks);
```

**2. 文档上传** — `controller/DocumentController.java`

用户上传文档（PDF/TXT），自动入库供后续检索。

**3. 检索增强** — `config/ChatModelConfig.java`

通过 `QuestionAnswerAdvisor` 自动完成"检索 → 注入"流程：

```java
QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
        .searchRequest(SearchRequest.builder().topK(5).build())
        .build();

ChatClient client = ChatClient.builder(chatModel)
        .defaultAdvisors(advisor)  // 自动检索相关文档并注入
        .build();
```

### 学习要点

- Embedding（嵌入）：将文本转为高维向量，语义相近的文本向量距离更近
- VectorStore：存储向量并提供相似度搜索能力
- `QuestionAnswerAdvisor` 是 Spring AI 的开箱即用方案，自动完成 RAG 流程
- `topK(5)` 表示检索最相关的 5 个文档片段

### 动手练习

1. 在 `docs/` 目录放入一个 txt 文件，重启项目
2. 开启 `useRag: true`，提问文档中的内容，验证 AI 能回答
3. 关闭 `useRag`，再问同样的问题，对比回答差异

---

## 课程五：Advisor（AI 拦截器）

**学习目标**：理解 Spring AI 的 Advisor 机制，类似 Servlet Filter 的请求/响应拦截。

### 核心概念

```
请求 → Advisor1 → Advisor2 → ... → 模型 → ... → Advisor2 → Advisor1 → 响应
```

### 已实现的 Advisor

**1. LoggingAdvisor** — 请求/响应日志

```java
@Override
public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    logRequest(request);                        // 打印请求摘要
    ChatClientResponse response = chain.nextCall(request);  // 调用下一个
    log.info("[AI 响应完成]");
    return response;
}
```

**2. ReReadingAdvisor** — Re2 技巧，让模型再读一遍问题

```java
// 在消息列表末尾追加: "Read the question again: <原始问题>"
// 研究表明这能提升模型对复杂问题的理解准确度
```

### 学习要点

- Advisor 同时支持同步（`CallAdvisor`）和流式（`StreamAdvisor`）
- `getOrder()` 控制执行顺序，数字越小越先执行
- 可以用来做：日志、缓存、敏感词过滤、Prompt 增强、Token 统计等

---

## 课程六：工具调用（Function Calling）

**学习目标**：理解如何让 AI 调用外部工具，突破纯文本的限制。

### 核心概念

```
用户: "今天北京天气如何？"
  → AI: 我需要调用天气查询工具
  → 系统执行工具，返回结果
  → AI: 基于结果生成回答
```

### 工具清单

| 工具名 | 功能 | 参数 |
|--------|------|------|
| `baiduSearch` | 百度搜索 | query: 搜索关键词 |
| `webScraper` | 网页抓取 | url: 目标 URL |
| `fileRead` | 文件读取 | filePath: 文件路径 |
| `fileWrite` | 文件写入 | filePath + content |
| `resourceDownload` | 资源下载 | url + savePath |
| `terminal` | 终端命令 | command: 命令字符串 |
| `pdfGeneration` | PDF 生成 | content + outputPath |
| `research` | 组合调研 | topic: 调研主题 |
| `terminate` | 任务终止 | finalAnswer: 最终结果 |

### 关键代码

**1. 定义工具** — 用 `@Tool` 注解标注方法

```java
@Component
public class BaiduSearchTool {
    @Tool(name = "baiduSearch", description = "Search Baidu by keyword")
    public String search(@ToolParam(description = "The search keyword") String query) {
        // 实际的搜索逻辑
    }
}
```

**2. 注册工具** — `config/ChatModelConfig.java`

```java
ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
        .toolObjects(baiduSearchTool, webScraperTool, ...)
        .build()
        .getToolCallbacks();
```

**3. 工具安全** — `tool/ToolValidator.java`

- 文件路径校验：限制在工作目录内，防止路径遍历
- URL 格式校验：只允许 http/https
- 终端命令黑名单：拦截 `rm -rf`、`shutdown` 等危险命令

### 学习要点

- `@Tool` 告诉框架这个方法可以被 AI 调用
- `@ToolParam` 的 `description` 会传给模型，帮助模型理解参数含义
- 模型不会直接执行代码，它只生成工具名和参数，由框架执行
- 工具返回 String，模型基于返回值继续推理

### 动手练习

1. 新建一个工具类，实现一个简单的计算器工具
2. 在 `ChatModelConfig` 中注册
3. 让 AI 调用你的工具完成一个计算任务

---

## 课程七：ReAct Agent 智能体

**学习目标**：理解 ReAct（Reasoning + Acting）模式，实现自主决策的智能体。

### 核心概念

ReAct 是一种让 LLM 交替进行**推理**和**行动**的模式：

```
┌─────────────────────────────────────────┐
│              ReAct 循环                  │
│                                         │
│   Thought（推理）: 我需要搜索 X          │
│       ↓                                 │
│   Action（行动）: 调用 baiduSearch(X)   │
│       ↓                                 │
│   Observation（观察）: 搜索结果是...     │
│       ↓                                 │
│   Thought（推理）: 信息不足，再抓取网页  │
│       ↓                                 │
│   Action（行动）: 调用 webScraper(url)  │
│       ↓                                 │
│   Observation（观察）: 页面内容是...     │
│       ↓                                 │
│   Thought（推理）: 信息已足够            │
│       ↓                                 │
│   Action（行动）: terminate(最终答案)    │
│                                         │
└─────────────────────────────────────────┘
```

### 关键代码

**1. Agent 循环** — `service/impl/AgentServiceImpl.java`

```java
while (round < MAX_AGENT_ROUNDS) {
    // 1. 调用模型
    ChatResponse response = chatModel.call(new Prompt(messages, chatOptions));
    AssistantMessage assistant = response.getResult().getOutput();

    // 2. 没有工具调用 → 直接回答，结束
    if (!assistant.hasToolCalls()) {
        return buildReply(assistant.getText());
    }

    // 3. 有工具调用 → 执行工具
    for (ToolCall toolCall : assistant.getToolCalls()) {
        String result = executeToolWithRetry(callback, toolCall);
        toolResponses.add(result);
    }

    // 4. 把工具结果追加到消息列表，进入下一轮
    messages.add(new ToolResponseMessage(toolResponses));
    messages.add(new UserMessage(NEXT_STEP_PROMPT));
}
```

**2. 任务规划** — `service/impl/PlanningServiceImpl.java`

Agent 执行前，先调用 LLM 拆解任务：

```java
// 用户任务: "调研 Java AI 框架并生成 PDF"
// LLM 返回:
//   1. 搜索 Java AI 框架
//   2. 抓取官方文档
//   3. 整理对比
//   4. 生成 PDF
```

规划结果注入 System Prompt，引导 Agent 按步骤执行。

**3. 错误恢复** — 工具失败时自动重试 + 动态提示词

```java
// 工具执行失败 → 自动重试 1 次
result = executeToolWithRetry(callback, toolName, arguments);

// 重试仍失败 → 注入错误引导提示词
String nextPrompt = lastRoundHadError
        ? NEXT_STEP_PROMPT_WITH_ERROR   // "分析失败原因，选择替代方案"
        : NEXT_STEP_PROMPT;             // 正常的下一步引导
```

**4. 组合工具** — `tool/ResearchTool.java`

将搜索 + 抓取 + 汇总封装为单个工具，减少 Agent 循环轮次：

```java
@Tool(name = "research", description = "搜索主题并自动阅读结果")
public String research(String topic) {
    String searchResult = searchTool.search(topic);     // 搜索
    List<String> urls = extractUrls(searchResult);       // 提取 URL
    for (String url : urls) {
        summaries.add(scraperTool.scrape(url));           // 逐个抓取
    }
    return combined;  // 返回汇总
}
```

### 完整执行流程

```
POST /api/chat/agent
  {"task": "搜索 Spring AI 最新版本并生成 PDF 报告"}

  ↓ 规划阶段
  LLM 拆解: [搜索版本信息, 抓取官方文档, 生成 PDF]

  ↓ 执行阶段（ReAct 循环）
  Round 1: research("Spring AI 最新版本")     → 搜索+抓取+汇总
  Round 2: terminate("最新版本是 1.0.0...")   → 完成

  ↓ 返回结果
  {
    "result": "Spring AI 最新版本是 1.0.0...",
    "rounds": 2,
    "successCount": 1,
    "failureCount": 0,
    "traces": [
      { "round": 1, "toolName": "research", "arguments": "...", "result": "..." },
      { "round": 2, "toolName": "terminate", "arguments": "...", "result": "..." }
    ]
  }
```

### 学习要点

- `internalToolExecutionEnabled(false)` 禁用框架的自动工具执行，手动控制循环
- Agent 的"智能"来自 LLM 的推理能力，代码只负责循环和工具执行
- `MAX_AGENT_ROUNDS = 20` 防止无限循环
- `terminate` 工具是 Agent 的终止信号
- 规划阶段用 `ChatClient`（单次调用），执行阶段用 `ChatModel`（手动循环）

### 动手练习

1. 调用 `POST /api/chat/agent`，任务: `"搜索今天的新闻"`
2. 观察 `traces` 中每轮的工具调用情况
3. 修改 `MAX_AGENT_ROUNDS` 为 3，测试简单任务是否仍能完成
4. 新增一个自定义工具，让 Agent 使用它完成任务

---

## 课程八：多模型注册与降级

**学习目标**：理解如何接入多个 AI 模型，并在主模型不可用时自动降级。

### 核心概念

```
请求 → ChatModelRegistry
         ├── qwen（通义千问）← 默认
         └── nvidia（Llama 3.1）← 降级备选
```

### 关键代码

**`service/ChatModelRegistry.java`**

```java
// 注册模型
registry.register("qwen", qwenClient, qwenChatModel);
registry.register("nvidia", nvidiaClient, nvidiaChatModel);

// 获取模型（自动降级）
ChatClient client = registry.getChatClient("qwen");
// qwen 不可用 → 自动切换到 nvidia
```

### 学习要点

- 不同模型可以通过不同协议接入（通义千问用 DashScope，Llama 用 OpenAI 兼容协议）
- 降级策略通过 `fallbackOrder` 配置
- 前端可以通过 `GET /api/chat/models` 获取可用模型列表

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- Node.js 18+（前端开发）

### 配置

1. 复制配置文件并填入 API Key：

```bash
cp src/main/resources/application-dev.yml src/main/resources/application-local.yml
```

2. 修改 `application-local.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: 你的通义千问API-Key
```

### 启动

```bash
# 后端
mvn spring-boot:run

# 前端（可选）
cd lin-ai-agent-ui
npm install
npm run dev
```

### 访问

- API 文档：`http://localhost:8080/doc.html`
- 同步对话：`POST /api/chat`
- 流式对话：`POST /api/chat/stream`
- Agent 执行：`POST /api/chat/agent`
- 模型列表：`GET /api/chat/models`
- 文档上传：`POST /api/documents/upload`

---

## 学习路线图

```
课程一（同步对话）
  ↓
课程二（流式对话 SSE）
  ↓
课程三（会话记忆）
  ↓
课程四（RAG 知识库）
  ↓
课程五（Advisor 拦截器）
  ↓
课程六（工具调用 Function Calling）
  ↓
课程七（ReAct Agent 智能体）  ← 核心课程
  ↓
课程八（多模型降级）
```

建议按顺序学习，每节课都基于前一节的知识。
