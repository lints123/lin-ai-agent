package com.lin.ai.agent.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 组合调研工具 - 自动搜索+抓取+汇总，减少 Agent 轮次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResearchTool {

    private static final int MAX_RESULTS = 3;
    private static final int MAX_SUMMARY_LENGTH = 8000;

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s)\"']+");

    private final BaiduSearchTool searchTool;
    private final WebScraperTool scraperTool;

    @Tool(name = "research", description = "搜索指定主题并自动阅读搜索结果页面，返回综合摘要。适用于需要快速了解某个话题的场景。")
    public String research(@ToolParam(description = "要调研的主题关键词") String topic) {
        log.info("[工具] 组合调研: {}", topic);

        // 1. 搜索
        String searchResult = searchTool.search(topic);
        if (searchResult.startsWith("搜索失败")) {
            return searchResult;
        }

        // 2. 提取 URL
        List<String> urls = extractUrls(searchResult);
        if (urls.isEmpty()) {
            return "搜索完成但未找到可访问的链接。\n\n搜索结果:\n" + searchResult;
        }

        // 3. 抓取 Top N 页面
        List<String> summaries = new ArrayList<>();
        int count = 0;
        for (String url : urls) {
            if (count >= MAX_RESULTS) break;

            log.info("[工具] 调研抓取 ({}/{}): {}", count + 1, MAX_RESULTS, url);
            String pageContent = scraperTool.scrape(url);

            // 跳过抓取失败的页面
            if (!pageContent.startsWith("抓取失败")) {
                summaries.add("=== 来源: " + url + " ===\n" + pageContent);
                count++;
            }
        }

        if (summaries.isEmpty()) {
            return "所有页面抓取失败。\n\n搜索结果:\n" + searchResult;
        }

        // 4. 拼接汇总
        String combined = String.join("\n\n", summaries);
        if (combined.length() > MAX_SUMMARY_LENGTH) {
            combined = combined.substring(0, MAX_SUMMARY_LENGTH) + "\n\n...(内容已截断)";
        }

        return combined;
    }

    /**
     * 从搜索结果文本中提取 URL
     */
    private List<String> extractUrls(String searchResult) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(searchResult);
        while (matcher.find()) {
            String url = matcher.group();
            // 过滤掉百度内部链接
            if (!url.contains("baidu.com") && !url.contains("baidustatic.com")) {
                urls.add(url);
            }
        }
        return urls;
    }
}
