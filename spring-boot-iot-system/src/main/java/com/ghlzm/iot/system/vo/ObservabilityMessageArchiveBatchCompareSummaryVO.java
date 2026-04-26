package com.ghlzm.iot.system.vo;

import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchCompareSummaryVO {

    private Long confirmedExpiredRows;
    private Long dryRunExpiredRows;
    private Long applyArchivedRows;
    private Long applyDeletedRows;
    private Long remainingExpiredRows;
    private Long deltaConfirmedVsDeleted;
    private Long deltaDryRunVsDeleted;
    private Boolean matched;
}
