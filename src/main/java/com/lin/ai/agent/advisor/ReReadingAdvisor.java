package com.lin.ai.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-Reading（Re2）Advisor：让大模型在回答前再阅读一次用户问题，提升理解准确度
 */
@Slf4j
public class ReReadingAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public String getName() {
        return "ReReadingAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(reRead(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(reRead(request));
    }

    private ChatClientRequest reRead(ChatClientRequest request) {
        List<Message> messages = request.prompt().getInstructions();

        // 取最后一条用户消息
        String userText = messages.stream()
                .filter(m -> m instanceof UserMessage)
                .map(Message::getText)
                .reduce((first, second) -> second)
                .orElse("");

        if (userText.isEmpty()) {
            return request;
        }

        // 追加 Re2 指令，让模型重新阅读问题
        List<Message> modified = new ArrayList<>(messages);
        modified.add(new UserMessage("Read the question again: " + userText));
        log.info("[Re2] 已追加重复阅读指令，消息数: {} -> {}", messages.size(), modified.size());

        return ChatClientRequest.builder()
                .prompt(new Prompt(modified))
                .context(request.context())
                .build();
    }
}
