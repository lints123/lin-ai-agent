package com.lin.ai.agent.tool;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 网页抓取工具
 */
@Slf4j
@Component
public class WebScraperTool {

    private static final int MAX_CONTENT_LENGTH = 5000;

    @Tool(name = "webScraper", description = "Fetch and extract the main text content from a given URL")
    public String scrape(@ToolParam(description = "The URL of the web page to scrape") String url) {
        log.info("[工具] 网页抓取: {}", url);

        String validationError = ToolValidator.validateUrl(url);
        if (validationError != null) {
            return validationError;
        }

        try {
            String html = HttpUtil.get(url);
            Document doc = Jsoup.parse(html);

            // 移除脚本、样式等无关内容
            doc.select("script, style, nav, footer, header, iframe").remove();

            String title = doc.title();
            String text = doc.body().text();

            // 截断
            if (text.length() > MAX_CONTENT_LENGTH) {
                text = text.substring(0, MAX_CONTENT_LENGTH) + "...(内容已截断)";
            }

            return String.format("标题: %s\n\n内容:\n%s", title, text);
        } catch (Exception e) {
            log.error("[工具] 网页抓取失败: {}", url, e);
            return "抓取失败: " + e.getMessage();
        }
    }
}
