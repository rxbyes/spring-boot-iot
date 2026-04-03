package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 自动化最近运行摘要
 */
@Data
public class AutomationResultRunSummaryVO {

    private String runId;
    private String reportPath;
    private String updatedAt;
    private AutomationResultSummaryVO summary;
    private List<String> failedScenarioIds;
    private List<String> relatedEvidenceFiles;
}
