package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 业务验收包最近一次结果摘要
 */
@Data
public class BusinessAcceptanceLatestResultVO {

    private String runId;
    private String status;
    private String updatedAt;
    private Integer passedModuleCount;
    private Integer failedModuleCount;
    private List<String> failedModuleNames;
}
