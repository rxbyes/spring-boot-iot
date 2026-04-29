package com.ghlzm.iot.system.vo;

import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchCompareTableVO {

    private String tableName;
    private String label;
    private Long dryRunExpiredRows;
    private Long applyArchivedRows;
    private Long applyDeletedRows;
    private Long applyRemainingExpiredRows;
    private Long deltaDryRunVsDeleted;
    private Boolean matched;
    private String reason;
}
