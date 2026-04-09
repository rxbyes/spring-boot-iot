package com.ghlzm.iot.common.event.governance;

public record GovernanceOpsAlertRecoveredEvent(
        Long tenantId,
        String alertType,
        String alertCode,
        String recoveryComment,
        Long operatorUserId
) {
}
