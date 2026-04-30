package com.lin.ai.agent.service.impl;

import com.lin.ai.agent.service.ChatModelRegistry;
import com.lin.ai.agent.service.PlanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务规划服务实现 - 调用 LLM 将任务拆解为有序子步骤
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanningServiceImpl implements PlanningService {

    private static final String PLANNING_PROMPT = """
            请将以下任务拆解为 2-5 个具体的执行步骤。
            每个步骤应该是单一操作（搜索、读取、写入等），不要包含复合操作。
            只返回编号列表，不要额外的解释。

            任务：%s
            """;

    private final ChatModelRegistry chatModelRegistry;

    @Override
    public List<String> decomposeTask(String taskDescription, String modelId) {
        log.info("[Planning] 开始规划任务: {}", taskDescription);

        ChatClient chatClient = chatModelRegistry.getChatClient(modelId);

        String planText = chatClient.prompt()
                .user(String.format(PLANNING_PROMPT, taskDescription))
                .call()
                .content();

        List<String> steps = parseSteps(planText);
        log.info("[Planning] 任务已拆解为 {} 个步骤", steps.size());

        return steps;
    }

    /**
     * 解析 LLM 返回的编号列表为步骤列表
     * 支持格式: "1. xxx" / "1) xxx" / "- xxx" / "* xxx"
     */
    private List<String> parseSteps(String planText) {
        if (planText == null || planText.isBlank()) {
            return List.of();
        }

        List<String> steps = new ArrayList<>();
        String[] lines = planText.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            // 匹配 "1. " "1) " "- " "* " 开头的行
            if (trimmed.matches("^(\\d+[.)\\s]|[-*]\\s).*")) {
                // 去掉前缀编号
                String step = trimmed.replaceFirst("^(\\d+[.)\\s]|[-*]\\s)\\s*", "").trim();
                if (!step.isEmpty()) {
                    steps.add(step);
                }
            }
        }

        return steps;
    }
}
