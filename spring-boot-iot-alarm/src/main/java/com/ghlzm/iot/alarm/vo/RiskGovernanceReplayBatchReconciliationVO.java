package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 发布批次快照对账摘要。
 */
@Data
public class RiskGovernanceReplayBatchReconciliationVO {

    private Long releaseBatchId;

    private Long approvalOrderId;

    private String releaseStatus;

    private String releaseReason;

    private String rollbackTime;

    private Boolean snapshotAvailable;

    private Boolean consistent;

    private Long beforeApplyFieldCount;

    private Long afterApplyFieldCount;

    private Long currentFormalFieldCount;

    private Long batchCatalogMetricCount;

    private Long missingCurrentFieldCount;

    private Long extraCurrentFieldCount;

    private String sampleMissingCurrentIdentifier;

    private String sampleExtraCurrentIdentifier;
}
