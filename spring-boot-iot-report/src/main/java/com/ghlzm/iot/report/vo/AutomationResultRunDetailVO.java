package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 自动化运行详情
 */
@Data
public class AutomationResultRunDetailVO {

    private String runId;
    private String reportPath;
    private String updatedAt;
    private String registryVersion;
    private Map<String, Object> options;
    private AutomationResultSummaryVO summary;
    private List<AutomationResultRunResultVO> results;
    private List<String> failedScenarioIds;
    private List<String> relatedEvidenceFiles;
}
