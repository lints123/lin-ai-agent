package com.lin.ai.agent.tool;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PDF 生成工具
 */
@Slf4j
@Component
public class PdfGenerationTool {

    @Tool(name = "pdfGeneration", description = "Generate a PDF file from the given text content")
    public String generatePdf(
            @ToolParam(description = "The text content to convert into a PDF") String content,
            @ToolParam(description = "The output file path for the generated PDF") String outputPath) {
        log.info("[工具] 生成 PDF: {}", outputPath);
        try {
            Path path = Paths.get(outputPath);
            if (path.getParent() != null) {
                java.nio.file.Files.createDirectories(path.getParent());
            }

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // 使用支持中文的字体
            Font font = new Font(Font.NORMAL, 12);

            // 按段落分割内容
            String[] paragraphs = content.split("\n");
            for (String para : paragraphs) {
                if (para.trim().startsWith("#")) {
                    // 标题行
                    Font titleFont = new Font(Font.BOLD, 16);
                    document.add(new Paragraph(para.replace("#", "").trim(), titleFont));
                } else if (!para.trim().isEmpty()) {
                    document.add(new Paragraph(para.trim(), font));
                }
            }

            document.close();
            return "PDF 生成成功: " + outputPath;
        } catch (Exception e) {
            log.error("[工具] PDF 生成失败: {}", outputPath, e);
            return "PDF 生成失败: " + e.getMessage();
        }
    }
}
