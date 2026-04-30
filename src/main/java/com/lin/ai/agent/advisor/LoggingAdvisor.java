package com.lin.ai.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话日志拦截器，打印请求摘要和响应完成标记，不打印完整响应内容
 */
@Slf4j
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public String getName() {
        return "LoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        logRequest(request);
        ChatClientResponse response = chain.nextCall(request);
        log.info("[AI 响应完成] call 模式");
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        logRequest(request);
        return chain.nextStream(request)
                .doOnComplete(() -> log.info("[AI 响应完成] stream 模式"));
    }

    private void logRequest(ChatClientRequest request) {
        List<Message> messages = request.prompt().getInstructions();
        String userText = messages.stream()
                .filter(m -> m instanceof UserMessage)
                .map(Message::getText)
                .reduce((first, second) -> second)
                .orElse("");

        String truncated = userText.length() > 100
                ? userText.substring(0, 100) + "..."
                : userText;

        log.info("[AI 请求] 消息数: {}, 用户消息: {}", messages.size(), truncated);
    }
}
