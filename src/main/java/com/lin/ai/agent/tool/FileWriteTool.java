package com.lin.ai.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件写入工具
 */
@Slf4j
@Component
public class FileWriteTool {

    @Tool(name = "fileWrite", description = "Write content to a file at the specified path, creating parent directories if needed")
    public String writeFile(
            @ToolParam(description = "The path of the file to write") String filePath,
            @ToolParam(description = "The content to write into the file") String content) {
        log.info("[工具] 写入文件: {}", filePath);

        String validationError = ToolValidator.validateFilePath(filePath);
        if (validationError != null) {
            return validationError;
        }

        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return "写入成功: " + filePath + " (" + content.length() + " 字符)";
        } catch (Exception e) {
            log.error("[工具] 写入文件失败: {}", filePath, e);
            return "写入失败: " + e.getMessage();
        }
    }
}
