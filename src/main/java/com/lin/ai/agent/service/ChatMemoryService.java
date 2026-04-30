package com.lin.ai.agent.service;

import com.lin.ai.agent.common.AiConstants;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMemoryService {

    private final int maxHistorySize;

    private final Map<String, Deque<Message>> memoryMap = new ConcurrentHashMap<>();

    public ChatMemoryService() {
        this(AiConstants.MAX_HISTORY_SIZE);
    }

    public ChatMemoryService(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * 获取指定会话的历史消息
     */
    public List<Message> getHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Collections.emptyList();
        }
        Deque<Message> history = memoryMap.get(conversationId);
        return history != null ? List.copyOf(history) : Collections.emptyList();
    }

    /**
     * 记录一轮对话（用户消息 + AI回复），超过上限时移除最早的消息
     */
    public void addExchange(String conversationId, String userMessage, String assistantMessage) {
        memoryMap.compute(conversationId, (id, deque) -> {
            if (deque == null) {
                deque = new LinkedList<>();
            }
            deque.addLast(new UserMessage(userMessage));
            deque.addLast(new AssistantMessage(assistantMessage));
            while (deque.size() > maxHistorySize) {
                deque.removeFirst();
            }
            return deque;
        });
    }
}
