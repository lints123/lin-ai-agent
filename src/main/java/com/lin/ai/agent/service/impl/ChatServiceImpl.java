package com.lin.ai.agent.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lin.ai.agent.common.AiConstants;
import com.lin.ai.agent.model.dto.ChatParam;
import com.lin.ai.agent.model.vo.ChatReply;
import com.lin.ai.agent.service.ChatMemoryService;
import com.lin.ai.agent.service.ChatModelRegistry;
import com.lin.ai.agent.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatModelRegistry chatModelRegistry;
    private final ChatMemoryService chatMemoryService;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;

    @Override
    public ChatReply chat(ChatParam param) {
        String conversationId = resolveConversationId(param.getConversationId());
        ChatClient chatClient = chatModelRegistry.getChatClient(param.getModelId());
        List<Message> messages = buildMessages(param.getMessage(), conversationId);

        var prompt = chatClient.prompt().messages(messages);
        if (Boolean.TRUE.equals(param.getUseRag())) {
            prompt = prompt.advisors(questionAnswerAdvisor);
        }

        String content = prompt.call().content();

        chatMemoryService.addExchange(conversationId, param.getMessage(), content);

        return ChatReply.builder()
                .content(content)
                .conversationId(conversationId)
                .build();
    }

    @Override
    public Flux<String> streamChat(ChatParam param) {
        String conversationId = resolveConversationId(param.getConversationId());
        ChatClient chatClient = chatModelRegistry.getChatClient(param.getModelId());
        List<Message> messages = buildMessages(param.getMessage(), conversationId);

        StringBuilder contentBuffer = new StringBuilder();

        var prompt = chatClient.prompt().messages(messages);
        if (Boolean.TRUE.equals(param.getUseRag())) {
            prompt = prompt.advisors(questionAnswerAdvisor);
        }

        return prompt.stream()
                .content()
                .filter(text -> text != null && !text.isEmpty())
                .doOnNext(contentBuffer::append)
                .doOnComplete(() -> chatMemoryService.addExchange(
                        conversationId, param.getMessage(), contentBuffer.toString()));
    }

    private String resolveConversationId(String conversationId) {
        return (conversationId != null && !conversationId.isBlank())
                ? conversationId
                : IdUtil.fastSimpleUUID();
    }

    private List<Message> buildMessages(String userMessage, String conversationId) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(AiConstants.SYSTEM_PROMPT));
        messages.addAll(chatMemoryService.getHistory(conversationId));
        messages.add(new UserMessage(userMessage));
        return messages;
    }
}
