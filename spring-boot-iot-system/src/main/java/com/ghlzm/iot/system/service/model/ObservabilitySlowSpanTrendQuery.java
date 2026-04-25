package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class ObservabilitySlowSpanTrendQuery {

    private String spanType;
    private String eventCode;
    private String domainCode;
    private String objectType;
    private String objectId;
    private String status;
    private Long minDurationMs;
    private String dateFrom;
    private String dateTo;
    private String bucket;
}
