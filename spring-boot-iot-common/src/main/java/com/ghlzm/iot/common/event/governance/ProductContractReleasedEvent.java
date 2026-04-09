package com.ghlzm.iot.common.event.governance;

import java.util.List;

public record ProductContractReleasedEvent(
        Long tenantId,
        Long productId,
        Long releaseBatchId,
        String scenarioCode,
        List<String> releasedIdentifiers,
        Long operatorUserId,
        Long approvalOrderId
) {
}
