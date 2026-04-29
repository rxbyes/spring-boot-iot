package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchOverviewQuery {

    private String sourceTable;
    private String dateFrom;
    private String dateTo;
}
