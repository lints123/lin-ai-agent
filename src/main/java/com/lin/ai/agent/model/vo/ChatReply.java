package com.lin.ai.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对话响应结果")
public class ChatReply {

    @Schema(description = "AI 回复的内容", example = "你好！我是智能AI助手，很高兴为您服务。")
    private String content;

    @Schema(description = "会话ID，用于多轮对话上下文关联", example = "conv-001")
    private String conversationId;

    @Schema(description = "使用的AI模型名称", example = "qwen-plus")
    private String model;
}
