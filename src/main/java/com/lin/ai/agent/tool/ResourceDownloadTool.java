package com.lin.ai.agent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 资源下载工具
 */
@Slf4j
@Component
public class ResourceDownloadTool {

    @Tool(name = "resourceDownload", description = "Download a resource from a given URL and save it to a local path")
    public String download(
            @ToolParam(description = "The URL of the resource to download") String url,
            @ToolParam(description = "The local file path to save the downloaded resource") String savePath) {
        log.info("[工具] 下载资源: {} -> {}", url, savePath);
        try {
            File file = new File(savePath);
            FileUtil.mkdir(file.getParentFile());
            HttpUtil.downloadFile(url, file);
            long size = file.length();
            return String.format("下载成功: %s (%.1f KB)", savePath, size / 1024.0);
        } catch (Exception e) {
            log.error("[工具] 下载资源失败: {}", url, e);
            return "下载失败: " + e.getMessage();
        }
    }
}
