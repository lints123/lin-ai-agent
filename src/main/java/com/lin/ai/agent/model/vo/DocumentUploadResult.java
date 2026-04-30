package com.lin.ai.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档上传结果")
public class DocumentUploadResult {

    @Schema(description = "文件名", example = "report.pdf")
    private String fileName;

    @Schema(description = "文档分块数量", example = "15")
    private int chunkCount;

    @Schema(description = "上传状态", example = "success")
    private String status;
}
