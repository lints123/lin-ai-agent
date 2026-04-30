package com.lin.ai.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChatModelRegistry {

    private final Map<String, ChatClient> clientMap = new ConcurrentHashMap<>();
    private final Map<String, ChatModel> modelMap = new ConcurrentHashMap<>();
    private final String defaultModelId;
    private final Map<String, String> fallbackOrder;

    public ChatModelRegistry(String defaultModelId, Map<String, String> fallbackOrder) {
        this.defaultModelId = defaultModelId;
        this.fallbackOrder = fallbackOrder;
    }

    public void register(String modelId, ChatClient chatClient, ChatModel chatModel) {
        clientMap.put(modelId, chatClient);
        modelMap.put(modelId, chatModel);
        log.info("注册 AI 模型: {}", modelId);
    }

    /**
     * 根据模型 ID 获取 ChatClient，未指定时使用默认模型，异常时降级
     */
    public ChatClient getChatClient(String modelId) {
        String targetId = resolveTargetId(modelId);
        ChatClient client = clientMap.get(targetId);
        if (client != null) {
            return client;
        }
        return fallbackClient(targetId);
    }

    /**
     * 根据模型 ID 获取 ChatModel（用于 Agent Loop 直接调用）
     */
    public ChatModel getChatModel(String modelId) {
        String targetId = resolveTargetId(modelId);
        ChatModel model = modelMap.get(targetId);
        if (model != null) {
            return model;
        }
        return fallbackModel(targetId);
    }

    public Set<String> getAvailableModels() {
        return clientMap.keySet();
    }

    public String getDefaultModelId() {
        return defaultModelId;
    }

    private String resolveTargetId(String modelId) {
        return (modelId != null && !modelId.isBlank()) ? modelId : defaultModelId;
    }

    private ChatClient fallbackClient(String targetId) {
        String fallback = fallbackOrder.get(targetId);
        if (fallback != null && clientMap.containsKey(fallback)) {
            log.warn("模型 [{}] 不可用，降级到 [{}]", targetId, fallback);
            return clientMap.get(fallback);
        }
        if (!clientMap.isEmpty()) {
            Map.Entry<String, ChatClient> entry = clientMap.entrySet().iterator().next();
            log.warn("模型 [{}] 不可用且无降级配置，使用 [{}]", targetId, entry.getKey());
            return entry.getValue();
        }
        throw new IllegalStateException("没有可用的 AI 模型");
    }

    private ChatModel fallbackModel(String targetId) {
        String fallback = fallbackOrder.get(targetId);
        if (fallback != null && modelMap.containsKey(fallback)) {
            log.warn("模型 [{}] 不可用，降级到 [{}]", targetId, fallback);
            return modelMap.get(fallback);
        }
        if (!modelMap.isEmpty()) {
            Map.Entry<String, ChatModel> entry = modelMap.entrySet().iterator().next();
            log.warn("模型 [{}] 不可用且无降级配置，使用 [{}]", targetId, entry.getKey());
            return entry.getValue();
        }
        throw new IllegalStateException("没有可用的 AI 模型");
    }
}
