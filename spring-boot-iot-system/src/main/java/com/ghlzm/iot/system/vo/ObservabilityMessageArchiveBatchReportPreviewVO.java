package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchReportPreviewVO {

    private String batchNo;
    private String sourceTable;
    private String status;
    private String confirmReportPath;
    private LocalDateTime confirmReportGeneratedAt;
    private Boolean available;
    private String reasonCode;
    private String reasonMessage;
    private String resolvedJsonPath;
    private String resolvedMarkdownPath;
    private Boolean markdownAvailable;
    private Boolean markdownTruncated;
    private String markdownPreview;
    private LocalDateTime fileLastModifiedAt;
    private Map<String, Object> summary;
    private List<ObservabilityMessageArchiveBatchReportTableSummaryVO> tableSummaries;
}
