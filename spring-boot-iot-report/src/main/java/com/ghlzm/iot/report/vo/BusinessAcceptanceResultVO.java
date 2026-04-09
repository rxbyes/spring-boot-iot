package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 业务验收结果聚合
 */
@Data
public class BusinessAcceptanceResultVO {

    private String status;
    private Integer passedModuleCount;
    private Integer failedModuleCount;
    private List<String> failedModuleNames;
    private String durationText;
    private String runId;
    private String jumpToAutomationResultsPath;
    private List<BusinessAcceptanceModuleResultVO> modules;
}
