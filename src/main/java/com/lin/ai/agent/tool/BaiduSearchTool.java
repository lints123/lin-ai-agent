package com.lin.ai.agent.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 百度搜索工具
 */
@Slf4j
@Component
public class BaiduSearchTool {

    @Tool(name = "baiduSearch", description = "Search Baidu by keyword and return a list of results including title, link and snippet")
    public String search(@ToolParam(description = "The search keyword") String query) {
        log.info("[工具] 百度搜索: {}", query);
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.baidu.com/s?wd=" + encodedQuery;
            String html = HttpUtil.get(url);
            Document doc = Jsoup.parse(html);

            Elements results = doc.select("div.result, div.c-container");
            List<String> items = new ArrayList<>();
            int count = 0;

            for (Element result : results) {
                if (count >= 10) break;

                Element titleEl = result.selectFirst("h3 a, .t a");
                Element snippetEl = result.selectFirst(".c-abstract, .c-span-last .content-right_8Zs40, span.content-right_8Zs40");

                String title = titleEl != null ? titleEl.text() : "";
                String href = titleEl != null ? titleEl.attr("href") : "";
                String snippet = snippetEl != null ? snippetEl.text() : "";

                if (StrUtil.isNotBlank(title)) {
                    items.add(String.format("%d. %s\n   链接: %s\n   摘要: %s",
                            count + 1, title, href, snippet));
                    count++;
                }
            }

            return items.isEmpty()
                    ? "未找到相关搜索结果"
                    : "搜索结果：\n" + String.join("\n\n", items);
        } catch (Exception e) {
            log.error("[工具] 百度搜索失败", e);
            return "搜索失败: " + e.getMessage();
        }
    }
}
