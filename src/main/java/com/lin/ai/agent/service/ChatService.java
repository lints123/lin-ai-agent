package com.lin.ai.agent.service;

import com.lin.ai.agent.model.dto.ChatParam;
import com.lin.ai.agent.model.vo.ChatReply;
import reactor.core.publisher.Flux;

public interface ChatService {

    ChatReply chat(ChatParam param);

    Flux<String> streamChat(ChatParam param);
}
