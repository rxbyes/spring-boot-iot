package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 自动化失败场景摘要。
 */
@Data
public class AutomationResultFailedScenarioVO {

    private String scenarioId;
    private String scenarioTitle;
    private String moduleCode;
    private String moduleName;
    private String runnerType;
    private String stepLabel;
    private String apiRef;
    private String pageAction;
    private AutomationFailureDiagnosisVO diagnosis;
}
