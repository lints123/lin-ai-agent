package com.lin.ai.agent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "对话请求参数")
public class ChatParam {

    @Schema(description = "用户输入的消息内容", example = "你好，请介绍一下你自己", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "会话ID，用于多轮对话上下文关联，首次对话可不传", example = "conv-001")
    private String conversationId;

    @Schema(description = "模型ID，不传则使用默认模型", example = "qwen")
    private String modelId;

    @Schema(description = "是否启用 RAG 知识库检索", example = "true")
    private Boolean useRag;
}
