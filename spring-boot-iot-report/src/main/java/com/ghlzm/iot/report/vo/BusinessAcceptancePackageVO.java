package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 业务验收包
 */
@Data
public class BusinessAcceptancePackageVO {

    private String packageCode;
    private String packageName;
    private String description;
    private List<String> targetRoles;
    private List<String> supportedEnvironments;
    private String defaultAccountTemplate;
    private List<BusinessAcceptancePackageModuleVO> modules;
    private BusinessAcceptanceLatestResultVO latestResult;
}
