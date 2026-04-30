package com.lin.ai.agent.service.impl;

import com.lin.ai.agent.model.vo.DocumentUploadResult;
import com.lin.ai.agent.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final SimpleVectorStore vectorStore;

    @Value("${rag.vector-store-path}")
    private String storePath;

    @Override
    public DocumentUploadResult processAndStore(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        // 1. Parse document
        List<Document> documents;
        if (originalFilename.toLowerCase().endsWith(".pdf")) {
            documents = parsePdf(file);
        } else {
            documents = parseTxt(file);
        }

        // 2. Split into chunks
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);

        // 3. Embed and store (EmbeddingModel called automatically)
        vectorStore.add(chunks);

        // 4. Persist to JSON
        saveStore();

        log.info("[RAG] Processed file: {}, chunks: {}", originalFilename, chunks.size());
        return DocumentUploadResult.builder()
                .fileName(originalFilename)
                .chunkCount(chunks.size())
                .status("success")
                .build();
    }

    private List<Document> parsePdf(MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile("rag-", ".pdf");
            file.transferTo(tempFile.toFile());
            try {
                PagePdfDocumentReader reader = new PagePdfDocumentReader(
                        new FileSystemResource(tempFile),
                        PdfDocumentReaderConfig.builder()
                                .withPagesPerDocument(1)
                                .build()
                );
                return reader.get();
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            throw new RuntimeException("PDF parsing failed: " + e.getMessage(), e);
        }
    }

    private List<Document> parseTxt(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return List.of(new Document(content));
        } catch (Exception e) {
            throw new RuntimeException("TXT parsing failed: " + e.getMessage(), e);
        }
    }

    private void saveStore() {
        try {
            File file = new File(storePath);
            file.getParentFile().mkdirs();
            vectorStore.save(file);
        } catch (Exception e) {
            log.error("[RAG] Failed to save vector store", e);
        }
    }

    @Override
    public int loadFromDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("[RAG] docs directory not found: {}", dir.getAbsolutePath());
            return 0;
        }

        File[] files = dir.listFiles(f -> {
            String name = f.getName().toLowerCase();
            return f.isFile() && (name.endsWith(".pdf") || name.endsWith(".txt"));
        });
        if (files == null || files.length == 0) {
            log.info("[RAG] No PDF/TXT files found in {}", dir.getAbsolutePath());
            return 0;
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        int totalChunks = 0;

        for (File file : files) {
            try {
                List<Document> documents = file.getName().toLowerCase().endsWith(".pdf")
                        ? parsePdfFile(file)
                        : parseTxtFile(file);

                List<Document> chunks = splitter.apply(documents);
                vectorStore.add(chunks);
                totalChunks += chunks.size();
                log.info("[RAG] Loaded file: {}, chunks: {}", file.getName(), chunks.size());
            } catch (Exception e) {
                log.error("[RAG] Failed to load file: {}", file.getName(), e);
            }
        }

        if (totalChunks > 0) {
            saveStore();
        }
        log.info("[RAG] Loaded {} files, {} total chunks from {}", files.length, totalChunks, dirPath);
        return totalChunks;
    }

    private List<Document> parsePdfFile(File file) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                new FileSystemResource(file),
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(1)
                        .build()
        );
        return reader.get();
    }

    private List<Document> parseTxtFile(File file) {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return List.of(new Document(content));
        } catch (Exception e) {
            throw new RuntimeException("TXT parsing failed: " + e.getMessage(), e);
        }
    }
}
