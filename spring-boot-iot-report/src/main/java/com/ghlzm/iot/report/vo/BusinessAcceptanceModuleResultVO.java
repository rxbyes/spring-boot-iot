package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 业务验收模块结果
 */
@Data
public class BusinessAcceptanceModuleResultVO {

    private String moduleCode;
    private String moduleName;
    private String status;
    private Integer failedScenarioCount;
    private List<String> failedScenarioTitles;
    private String suggestedDirection;
    private List<BusinessAcceptanceFailureDetailVO> failureDetails;
}
