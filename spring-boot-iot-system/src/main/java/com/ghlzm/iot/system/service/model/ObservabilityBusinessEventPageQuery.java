package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class ObservabilityBusinessEventPageQuery {

    private String traceId;
    private String eventCode;
    private String domainCode;
    private String actionCode;
    private String objectType;
    private String objectId;
    private String resultStatus;
    private String dateFrom;
    private String dateTo;
    private Long pageNum;
    private Long pageSize;
}
