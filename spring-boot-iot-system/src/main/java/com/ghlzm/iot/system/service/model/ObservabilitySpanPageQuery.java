package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class ObservabilitySpanPageQuery {

    private String traceId;
    private String spanType;
    private String eventCode;
    private String domainCode;
    private String objectType;
    private String objectId;
    private String status;
    private Long minDurationMs;
    private String dateFrom;
    private String dateTo;
    private Long pageNum;
    private Long pageSize;
}
