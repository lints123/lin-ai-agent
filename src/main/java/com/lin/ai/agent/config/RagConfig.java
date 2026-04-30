package com.lin.ai.agent.config;

import com.lin.ai.agent.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.File;

@Slf4j
@Configuration
public class RagConfig {

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel,
                                         @Value("${rag.vector-store-path}") String storePath) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File file = new File(storePath);
        if (file.exists()) {
            store.load(file);
        }
        return store;
    }

    @Bean
    public CommandLineRunner ragDocsLoader(@Lazy DocumentService documentService,
                                           @Value("${rag.vector-store-path}") String storePath,
                                           @Value("${rag.docs-path}") String docsPath) {
        return args -> {
            File storeFile = new File(storePath);
            if (storeFile.exists()) {
                log.info("[RAG] Vector store already exists, skip loading docs/");
                return;
            }
            int chunks = documentService.loadFromDirectory(docsPath);
            if (chunks > 0) {
                log.info("[RAG] Initial load complete: {} chunks", chunks);
            }
        };
    }
}
