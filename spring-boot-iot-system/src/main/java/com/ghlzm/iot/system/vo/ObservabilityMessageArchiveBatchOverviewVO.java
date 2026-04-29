package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchOverviewVO {

    private Long totalBatches;
    private Long matchedBatches;
    private Long driftedBatches;
    private Long partialBatches;
    private Long unavailableBatches;
    private Long abnormalBatches;
    private Long totalDeltaConfirmedVsDeleted;
    private Long totalRemainingExpiredRows;
    private String latestAbnormalBatch;
    private LocalDateTime latestAbnormalOccurredAt;
}
