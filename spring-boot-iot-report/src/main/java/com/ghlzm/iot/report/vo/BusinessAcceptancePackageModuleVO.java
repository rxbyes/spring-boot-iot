package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 业务验收包模块
 */
@Data
public class BusinessAcceptancePackageModuleVO {

    private String moduleCode;
    private String moduleName;
    private String suggestedDirection;
    private List<String> scenarioRefs;
}
