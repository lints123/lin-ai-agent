package com.lin.ai.agent.tool;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工具输入校验器 + ThreadLocal workDir 管理
 */
public final class ToolValidator {

    private static final ThreadLocal<String> CURRENT_WORK_DIR = new ThreadLocal<>();

    private ToolValidator() {
    }

    /**
     * 设置当前请求的工作目录（由 AgentServiceImpl 调用）
     */
    public static void setWorkDir(String workDir) {
        CURRENT_WORK_DIR.set(workDir);
    }

    /**
     * 获取当前请求的工作目录
     */
    public static String getWorkDir() {
        return CURRENT_WORK_DIR.get();
    }

    /**
     * 清除当前请求的工作目录（finally 中调用）
     */
    public static void clearWorkDir() {
        CURRENT_WORK_DIR.remove();
    }

    /**
     * 校验文件路径是否在工作目录下，防止路径遍历攻击。
     *
     * @return null 表示通过，非 null 为错误信息
     */
    public static String validateFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "文件路径不能为空";
        }

        String workDir = getWorkDir();
        if (workDir == null || workDir.isBlank()) {
            // 未配置 workDir，不做限制
            return null;
        }

        try {
            Path resolved = Paths.get(workDir).resolve(filePath).normalize().toAbsolutePath();
            Path allowedBase = Paths.get(workDir).normalize().toAbsolutePath();

            if (!resolved.startsWith(allowedBase)) {
                return "文件路径超出工作目录限制: " + workDir;
            }
        } catch (Exception e) {
            return "文件路径格式无效: " + filePath;
        }

        return null;
    }

    /**
     * 校验 URL 格式是否合法。
     *
     * @return null 表示通过，非 null 为错误信息
     */
    public static String validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return "URL 不能为空";
        }

        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "URL 必须以 http:// 或 https:// 开头";
        }

        return null;
    }
}
