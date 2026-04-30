package com.lin.ai.agent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Agent 任务请求参数")
public class AgentParam {

    @Schema(description = "任务描述", example = "搜索 Spring AI 最新版本并生成 PDF 报告",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String task;

    @Schema(description = "模型 ID，不指定则使用默认模型", example = "qwen")
    private String modelId;

    @Schema(description = "工作目录（文件操作的根路径）", example = "C:/workspace")
    private String workDir;
}
