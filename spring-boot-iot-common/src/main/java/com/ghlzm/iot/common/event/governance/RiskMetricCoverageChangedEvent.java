package com.ghlzm.iot.common.event.governance;

public record RiskMetricCoverageChangedEvent(
        Long tenantId,
        Long productId,
        Long releaseBatchId,
        Long missingBindingCount,
        Long missingRuleCount,
        Long missingLinkageCount,
        Long missingPlanCount,
        Long operatorUserId
) {
}
