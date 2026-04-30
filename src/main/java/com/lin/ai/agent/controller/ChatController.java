package com.lin.ai.agent.controller;

import com.lin.ai.agent.common.Result;
import com.lin.ai.agent.model.dto.AgentParam;
import com.lin.ai.agent.model.dto.ChatParam;
import com.lin.ai.agent.model.vo.AgentReply;
import com.lin.ai.agent.model.vo.ChatReply;
import com.lin.ai.agent.service.AgentService;
import com.lin.ai.agent.service.ChatModelRegistry;
import com.lin.ai.agent.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Set;

@Tag(name = "AI 对话")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AgentService agentService;
    private final ChatModelRegistry chatModelRegistry;

    @Operation(summary = "同步对话", description = "发送消息，等待完整回复")
    @PostMapping
    public Result<ChatReply> chat(@RequestBody ChatParam param) {
        return Result.success(chatService.chat(param));
    }

    @Operation(summary = "流式对话", description = "发送消息，以 SSE 流式返回回复")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatParam param) {
        return chatService.streamChat(param);
    }

    @Operation(summary = "获取可用模型列表", description = "返回当前已注册的所有 AI 模型 ID")
    @GetMapping("/models")
    public Result<Set<String>> listModels() {
        return Result.success(chatModelRegistry.getAvailableModels());
    }

    @Operation(summary = "Agent 模式", description = "提交任务由 Agent 自主使用工具完成")
    @PostMapping("/agent")
    public Result<AgentReply> agent(@RequestBody AgentParam param) {
        return Result.success(agentService.execute(param));
    }
}
