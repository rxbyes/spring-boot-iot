package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 自动化失败归因诊断。
 */
@Data
public class AutomationFailureDiagnosisVO {

    private String category;
    private String reason;
    private String evidenceSummary;
}
