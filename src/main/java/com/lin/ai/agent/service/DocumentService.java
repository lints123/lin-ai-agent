package com.lin.ai.agent.service;

import com.lin.ai.agent.model.vo.DocumentUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentUploadResult processAndStore(MultipartFile file);

    /**
     * 扫描指定目录下的 PDF/TXT 文件，解析并加载到向量库。
     *
     * @return 加载的文档分块总数
     */
    int loadFromDirectory(String dirPath);
}
