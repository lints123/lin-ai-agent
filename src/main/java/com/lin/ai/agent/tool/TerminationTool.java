package com.lin.ai.agent.tool;

import com.lin.ai.agent.common.AiConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 终止工具 - Agent 调用此工具表示任务完成
 */
@Slf4j
@Component
public class TerminationTool {

    @Tool(name = "terminate", description = "Call this tool when the task is finished or cannot proceed. Attach the final result summary.")
    public String terminate(@ToolParam(description = "The final answer or result summary of the task") String finalAnswer) {
        log.info("[工具] 终止工具被调用: {}", finalAnswer);
        return AiConstants.TERMINATION_SIGNAL;
    }
}
