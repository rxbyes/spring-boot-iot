package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchPageQuery {

    private String batchNo;
    private String sourceTable;
    private String status;
    private String compareStatus;
    private Boolean onlyAbnormal;
    private String dateFrom;
    private String dateTo;
    private Long pageNum;
    private Long pageSize;
}
