package com.ghlzm.iot.system.vo;

import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchReportTableSummaryVO {

    private String tableName;
    private String label;
    private Integer retentionDays;
    private String timeField;
    private String cutoffAt;
    private Long totalRows;
    private Long expiredRows;
    private Long deletedRows;
    private Long remainingExpiredRows;
    private String earliestRecordAt;
    private String latestRecordAt;
}
