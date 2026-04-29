package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class ObservabilityScheduledTaskPageQuery {

    private String traceId;
    private String domainCode;
    private String taskCode;
    private String triggerType;
    private String status;
    private Long minDurationMs;
    private String dateFrom;
    private String dateTo;
    private Long pageNum;
    private Long pageSize;
}
