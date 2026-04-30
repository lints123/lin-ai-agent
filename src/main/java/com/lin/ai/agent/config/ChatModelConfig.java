package com.lin.ai.agent.config;

import com.lin.ai.agent.advisor.LoggingAdvisor;
import com.lin.ai.agent.advisor.ReReadingAdvisor;
import com.lin.ai.agent.service.ChatModelRegistry;
import com.lin.ai.agent.tool.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ChatModelConfig {

    @Value("${ai.models.default-model:qwen}")
    private String defaultModel;

    @Value("${ai.models.nvidia.base-url:}")
    private String nvidiaBaseUrl;

    @Value("${ai.models.nvidia.api-key:}")
    private String nvidiaApiKey;

    @Value("${ai.models.nvidia.model:nvidia/llama-3.1-nemotron-70b-instruct}")
    private String nvidiaModel;

    private final LoggingAdvisor loggingAdvisor = new LoggingAdvisor();
    private final ReReadingAdvisor reReadingAdvisor = new ReReadingAdvisor();

    @Bean
    public ChatModelRegistry chatModelRegistry(ChatModel dashScopeChatModel,
                                                SimpleVectorStore vectorStore) {
        Map<String, String> fallbackOrder = Map.of(
                "qwen", "nvidia",
                "nvidia", "qwen"
        );

        ChatModelRegistry registry = new ChatModelRegistry(defaultModel, fallbackOrder);

        // 注册千问
        ChatClient qwenClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(reReadingAdvisor, loggingAdvisor)
                .build();
        registry.register("qwen", qwenClient, dashScopeChatModel);

        // 注册 NVIDIA
        if (nvidiaBaseUrl != null && !nvidiaBaseUrl.isBlank()
                && nvidiaApiKey != null && !nvidiaApiKey.isBlank()
                && !nvidiaApiKey.equals("your-nvidia-api-key")) {
            OpenAiApi nvidiaApi = OpenAiApi.builder()
                    .baseUrl(nvidiaBaseUrl)
                    .apiKey(nvidiaApiKey)
                    .build();

            OpenAiChatModel nvidiaChatModel = OpenAiChatModel.builder()
                    .openAiApi(nvidiaApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(nvidiaModel)
                            .build())
                    .build();

            ChatClient nvidiaClient = ChatClient.builder(nvidiaChatModel)
                    .defaultAdvisors(reReadingAdvisor, loggingAdvisor)
                    .build();
            registry.register("nvidia", nvidiaClient, nvidiaChatModel);
        }

        return registry;
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(SimpleVectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(5).build())
                .build();
    }

    @Bean
    public Map<String, ToolCallback> toolCallbackMap(
            BaiduSearchTool baiduSearchTool,
            WebScraperTool webScraperTool,
            FileReadTool fileReadTool,
            FileWriteTool fileWriteTool,
            ResourceDownloadTool resourceDownloadTool,
            TerminalTool terminalTool,
            PdfGenerationTool pdfGenerationTool,
            TerminationTool terminationTool,
            ResearchTool researchTool) {

        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(baiduSearchTool, webScraperTool, fileReadTool,
                        fileWriteTool, resourceDownloadTool, terminalTool,
                        pdfGenerationTool, terminationTool, researchTool)
                .build()
                .getToolCallbacks();

        Map<String, ToolCallback> map = new HashMap<>();
        for (ToolCallback cb : callbacks) {
            map.put(cb.getToolDefinition().name(), cb);
        }
        return map;
    }
}
