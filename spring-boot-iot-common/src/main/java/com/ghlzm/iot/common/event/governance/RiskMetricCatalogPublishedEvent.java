package com.ghlzm.iot.common.event.governance;

import java.util.List;

public record RiskMetricCatalogPublishedEvent(
        Long tenantId,
        Long productId,
        Long releaseBatchId,
        List<Long> publishedRiskMetricIds,
        List<Long> retiredRiskMetricIds,
        Long operatorUserId
) {
}
