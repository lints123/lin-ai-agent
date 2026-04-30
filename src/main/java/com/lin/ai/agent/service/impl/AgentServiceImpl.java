package com.lin.ai.agent.service.impl;

import com.lin.ai.agent.common.AiConstants;
import com.lin.ai.agent.model.dto.AgentParam;
import com.lin.ai.agent.model.vo.AgentReply;
import com.lin.ai.agent.service.AgentService;
import com.lin.ai.agent.service.ChatModelRegistry;
import com.lin.ai.agent.service.PlanningService;
import com.lin.ai.agent.tool.ToolValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 服务实现 - 自定义 Agent Loop
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final ChatModelRegistry chatModelRegistry;
    private final Map<String, ToolCallback> toolCallbackMap;
    private final PlanningService planningService;

    @Override
    public AgentReply execute(AgentParam param) {
        // 设置当前请求的 workDir（供工具校验使用）
        ToolValidator.setWorkDir(param.getWorkDir());
        try {
            return doExecute(param);
        } finally {
            ToolValidator.clearWorkDir();
        }
    }

    private AgentReply doExecute(AgentParam param) {
        ChatModel chatModel = chatModelRegistry.getChatModel(param.getModelId());

        // 构建工具选项（禁用自动执行，由我们自己控制循环）
        ToolCallingChatOptions chatOptions = DefaultToolCallingChatOptions.builder()
                .toolCallbacks(new ArrayList<>(toolCallbackMap.values()))
                .internalToolExecutionEnabled(false)
                .build();

        // 规划阶段：拆解任务为子步骤
        List<String> plan = planTask(param);
        String planText = plan.isEmpty() ? "" : plan.stream()
                .map(s -> (plan.indexOf(s) + 1) + ". " + s)
                .collect(Collectors.joining("\n"));

        // 构建增强版 System Prompt（含执行计划）
        String systemPrompt = AiConstants.AGENT_SYSTEM_PROMPT;
        if (!planText.isEmpty()) {
            systemPrompt += "\n\n## 执行计划\n请按以下步骤执行任务，但可以根据实际情况调整：\n" + planText;
        }

        // 初始化消息列表
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(param.getTask()));

        List<AgentReply.ToolCallTrace> traces = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        boolean lastRoundHadError = false;

        log.info("[Agent] 开始执行任务: {}", param.getTask());

        int round = 0;
        while (round < AiConstants.MAX_AGENT_ROUNDS) {
            round++;
            final int currentRound = round;
            log.info("[Agent] === 第 {} 轮 ===", currentRound);

            ChatResponse response = chatModel.call(new Prompt(messages, chatOptions));
            AssistantMessage assistant = response.getResult().getOutput();

            // 捕获 Thought（模型在调用工具前输出的推理文本）
            String thought = assistant.getText();
            if (thought != null && !thought.isBlank()) {
                log.info("[Agent] Thought: {}", thought.length() > 200
                        ? thought.substring(0, 200) + "..." : thought);
            }

            messages.add(assistant);

            // 没有工具调用 → 模型直接回答，循环结束
            if (!assistant.hasToolCalls()) {
                log.info("[Agent] 任务完成（直接回答），共 {} 轮", currentRound);
                return AgentReply.builder()
                        .result(assistant.getText())
                        .rounds(currentRound)
                        .successCount(successCount)
                        .failureCount(failureCount)
                        .traces(traces)
                        .build();
            }

            // 处理工具调用
            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
            boolean terminated = false;
            String finalAnswer = null;

            for (AssistantMessage.ToolCall toolCall : assistant.getToolCalls()) {
                String toolName = toolCall.name();
                String arguments = toolCall.arguments();

                log.info("[Agent] 调用工具: {}({})", toolName, arguments);

                // 检查终止工具
                if ("terminate".equals(toolName)) {
                    finalAnswer = extractFinalAnswer(arguments);
                    terminated = true;
                    break;
                }

                // 执行工具（失败自动重试 1 次）
                ToolCallback callback = toolCallbackMap.get(toolName);
                String result;
                if (callback == null) {
                    result = "工具未找到: " + toolName;
                    failureCount++;
                } else {
                    result = executeToolWithRetry(callback, toolName, arguments);
                    if (isToolError(result)) {
                        failureCount++;
                    } else {
                        successCount++;
                    }
                }

                // 截断记录
                String truncatedResult = result.length() > 500
                        ? result.substring(0, 500) + "..." : result;
                String truncatedArgs = arguments.length() > 200
                        ? arguments.substring(0, 200) + "..." : arguments;

                traces.add(AgentReply.ToolCallTrace.builder()
                        .round(currentRound)
                        .toolName(toolName)
                        .arguments(truncatedArgs)
                        .result(truncatedResult)
                        .build());

                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        toolCall.id(), toolName, result));
            }

            // 终止信号
            if (terminated) {
                log.info("[Agent] 任务完成（终止工具），共 {} 轮", currentRound);
                return AgentReply.builder()
                        .result(finalAnswer)
                        .rounds(currentRound)
                        .successCount(successCount)
                        .failureCount(failureCount)
                        .traces(traces)
                        .build();
            }

            // 追加工具响应
            messages.add(ToolResponseMessage.builder()
                    .responses(toolResponses)
                    .build());

            // 根据是否有失败，动态选择下一步提示词
            lastRoundHadError = failureCount > 0
                    && traces.stream().anyMatch(t -> t.getRound() == currentRound && isToolError(t.getResult()));
            String nextPrompt = lastRoundHadError
                    ? AiConstants.NEXT_STEP_PROMPT_WITH_ERROR
                    : AiConstants.NEXT_STEP_PROMPT;
            messages.add(new UserMessage(nextPrompt));
        }

        // 超过最大轮次
        log.warn("[Agent] 达到最大轮次限制: {}", AiConstants.MAX_AGENT_ROUNDS);
        return AgentReply.builder()
                .result("任务执行达到最大轮次限制（" + AiConstants.MAX_AGENT_ROUNDS + "轮），未能完成。")
                .rounds(AiConstants.MAX_AGENT_ROUNDS)
                .successCount(successCount)
                .failureCount(failureCount)
                .traces(traces)
                .build();
    }

    /**
     * 规划任务：拆解为子步骤。失败时返回空列表，不影响 Agent 正常执行。
     */
    private List<String> planTask(AgentParam param) {
        try {
            return planningService.decomposeTask(param.getTask(), param.getModelId());
        } catch (Exception e) {
            log.warn("[Agent] 任务规划失败，将跳过规划阶段: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 从终止工具参数中提取最终答案
     */
    private String extractFinalAnswer(String arguments) {
        // arguments 可能是 JSON 格式 {"finalAnswer":"xxx"} 或纯文本
        if (arguments.contains("\"finalAnswer\"")) {
            try {
                // 简单提取，不引入 JSON 解析库
                int start = arguments.indexOf("\"finalAnswer\"")
                        + "\"finalAnswer\"".length();
                // 跳过冒号和可能的空格、引号
                while (start < arguments.length()
                        && ": \t".indexOf(arguments.charAt(start)) >= 0) {
                    start++;
                }
                if (start < arguments.length() && arguments.charAt(start) == '"') {
                    start++;
                }
                int end = arguments.lastIndexOf("\"");
                if (end > start) {
                    return arguments.substring(start, end);
                }
            } catch (Exception e) {
                // 解析失败，返回原始参数
            }
        }
        return arguments;
    }

    /**
     * 执行工具，失败时自动重试 1 次
     */
    private String executeToolWithRetry(ToolCallback callback, String toolName, String arguments) {
        try {
            String result = callback.call(arguments);
            if (!isToolError(result)) {
                return result;
            }
            // 结果包含错误标识，重试一次
            log.warn("[Agent] 工具返回错误，重试: {}", toolName);
            return callback.call(arguments);
        } catch (Exception e) {
            // 异常重试一次
            log.warn("[Agent] 工具执行异常，重试: {}", toolName);
            try {
                return callback.call(arguments);
            } catch (Exception retryEx) {
                log.error("[Agent] 工具重试仍失败: {}", toolName, retryEx);
                return "工具执行失败: " + retryEx.getMessage();
            }
        }
    }

    /**
     * 判断工具返回结果是否为错误
     */
    private boolean isToolError(String result) {
        if (result == null) return true;
        String lower = result.toLowerCase();
        return lower.startsWith("工具执行失败")
                || lower.startsWith("工具未找到")
                || lower.startsWith("搜索失败")
                || lower.startsWith("抓取失败")
                || lower.startsWith("读取失败")
                || lower.startsWith("写入失败")
                || lower.startsWith("下载失败")
                || lower.startsWith("执行失败")
                || lower.startsWith("pdf 生成失败")
                || lower.startsWith("blocked");
    }
}
