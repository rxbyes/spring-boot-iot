package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 自动化失败模块摘要。
 */
@Data
public class AutomationResultFailedModuleVO {

    private String moduleCode;
    private String moduleName;
    private Integer failedScenarioCount;
    private AutomationFailureDiagnosisVO diagnosis;
}
