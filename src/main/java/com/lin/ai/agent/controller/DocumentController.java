package com.lin.ai.agent.controller;

import com.lin.ai.agent.common.Result;
import com.lin.ai.agent.model.vo.DocumentUploadResult;
import com.lin.ai.agent.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文档管理")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "上传文档", description = "上传 PDF 或 TXT 文件，解析后存入向量数据库用于 RAG 检索")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<DocumentUploadResult> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.fail("文件大小不能超过 10MB");
        }
        String name = file.getOriginalFilename();
        if (name == null || (!name.toLowerCase().endsWith(".pdf") && !name.toLowerCase().endsWith(".txt"))) {
            return Result.fail("仅支持 PDF 和 TXT 格式");
        }
        return Result.success(documentService.processAndStore(file));
    }
}
