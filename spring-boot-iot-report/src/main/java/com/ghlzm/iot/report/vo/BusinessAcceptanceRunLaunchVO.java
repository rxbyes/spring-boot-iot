package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 业务验收运行启动响应
 */
@Data
public class BusinessAcceptanceRunLaunchVO {

    private String jobId;
    private String status;
    private String runId;
    private String startedAt;
    private String errorMessage;
}
