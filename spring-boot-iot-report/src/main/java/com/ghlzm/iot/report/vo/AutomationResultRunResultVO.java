package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 自动化单场景执行结果
 */
@Data
public class AutomationResultRunResultVO {

    private String scenarioId;
    private String runnerType;
    private String status;
    private String blocking;
    private String summary;
    private List<String> evidenceFiles;
    private Map<String, Object> details;
    private AutomationFailureDiagnosisVO diagnosis;
}
