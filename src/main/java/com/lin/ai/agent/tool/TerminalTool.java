package com.lin.ai.agent.tool;

import com.lin.ai.agent.common.AiConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * 终端命令执行工具（带安全黑名单）
 */
@Slf4j
@Component
public class TerminalTool {

    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_OUTPUT_LENGTH = 5000;

    @Tool(name = "terminal", description = "Execute a terminal command and return the output. Dangerous commands are blocked by a blacklist.")
    public String execute(@ToolParam(description = "The terminal command to execute") String command) {
        log.info("[工具] 终端命令: {}", command);

        // 安全检查：黑名单
        String lower = command.toLowerCase();
        for (String dangerous : AiConstants.DANGEROUS_COMMANDS) {
            if (lower.contains(dangerous.toLowerCase())) {
                log.warn("[工具] 危险命令被拦截: {}", command);
                return "BLOCKED: 危险命令已被拦截 - " + dangerous;
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder();
            // Windows 用 cmd /c，其他平台用 sh -c
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "命令执行超时（" + TIMEOUT_SECONDS + "秒）";
            }

            String result = output.toString().trim();
            if (result.length() > MAX_OUTPUT_LENGTH) {
                result = result.substring(0, MAX_OUTPUT_LENGTH) + "...(输出已截断)";
            }

            int exitCode = process.exitValue();
            log.info("[工具] 命令执行完成, exitCode={}", exitCode);
            return result.isEmpty() ? "命令执行完成（无输出）" : result;
        } catch (Exception e) {
            log.error("[工具] 命令执行失败: {}", command, e);
            return "执行失败: " + e.getMessage();
        }
    }
}
