package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 自动化结果归档索引。
 */
@Data
public class AutomationResultArchiveIndexVO {

    private String generatedAt;
    private String resultsDir;
    private SourceSummary sourceSummary;
    private AutomationResultArchiveFacetVO facets;
    private List<RunRecord> runs;
    private List<SkippedFile> skippedFiles;

    @Data
    public static class SourceSummary {

        private Integer registryRunFiles;
        private Integer indexedRuns;
        private Integer skippedFiles;
    }

    @Data
    public static class RunRecord {

        private String runId;
        private String updatedAt;
        private String reportPath;
        private String status;
        private AutomationResultSummaryVO summary;
        private String packageCode;
        private String environmentCode;
        private List<String> runnerTypes;
        private List<String> failedScenarioIds;
        private List<String> relatedEvidenceFiles;
        private List<EvidenceItem> evidenceItems;
    }

    @Data
    public static class EvidenceItem {

        private String path;
        private String fileName;
        private String category;
        private String source;
    }

    @Data
    public static class SkippedFile {

        private String fileName;
        private String reason;
    }
}
