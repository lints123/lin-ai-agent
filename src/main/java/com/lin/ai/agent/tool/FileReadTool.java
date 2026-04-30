package com.lin.ai.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件读取工具
 */
@Slf4j
@Component
public class FileReadTool {

    private static final int MAX_CONTENT_LENGTH = 10000;

    @Tool(name = "fileRead", description = "Read the content of a file at the specified path and return it as text")
    public String readFile(@ToolParam(description = "The path of the file to read") String filePath) {
        log.info("[工具] 读取文件: {}", filePath);

        String validationError = ToolValidator.validateFilePath(filePath);
        if (validationError != null) {
            return validationError;
        }

        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                return "文件不存在: " + filePath;
            }

            String content = Files.readString(path);

            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH) + "...(内容已截断)";
            }

            return content;
        } catch (Exception e) {
            log.error("[工具] 读取文件失败: {}", filePath, e);
            return "读取失败: " + e.getMessage();
        }
    }
}
