package com.lin.ai.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent 执行结果")
public class AgentReply {

    @Schema(description = "最终执行结果", example = "任务完成：已生成 PDF 报告")
    private String result;

    @Schema(description = "执行轮次", example = "5")
    private int rounds;

    @Schema(description = "工具调用成功次数", example = "3")
    private int successCount;

    @Schema(description = "工具调用失败次数", example = "1")
    private int failureCount;

    @Schema(description = "工具调用链路追踪")
    private List<ToolCallTrace> traces;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单次工具调用记录")
    public static class ToolCallTrace {

        @Schema(description = "轮次编号", example = "1")
        private int round;

        @Schema(description = "工具名称", example = "baiduSearch")
        private String toolName;

        @Schema(description = "调用参数（截断）", example = "{\"query\":\"Spring AI\"}")
        private String arguments;

        @Schema(description = "执行结果（截断）", example = "找到 10 条搜索结果...")
        private String result;
    }
}
