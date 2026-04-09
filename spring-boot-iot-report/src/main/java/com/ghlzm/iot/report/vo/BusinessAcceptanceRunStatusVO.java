package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 业务验收运行状态
 */
@Data
public class BusinessAcceptanceRunStatusVO {

    private String jobId;
    private String status;
    private String runId;
    private String startedAt;
    private String finishedAt;
    private String errorMessage;
}
