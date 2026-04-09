package com.ghlzm.iot.system.listener;

import com.ghlzm.iot.common.event.governance.GovernanceOpsAlertRaisedEvent;
import com.ghlzm.iot.common.event.governance.GovernanceOpsAlertRecoveredEvent;
import com.ghlzm.iot.system.service.GovernanceOpsAlertService;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GovernanceOpsAlertEventListener {

    private static final Long SYSTEM_OPERATOR_ID = 1L;

    private final GovernanceOpsAlertService governanceOpsAlertService;

    public GovernanceOpsAlertEventListener(GovernanceOpsAlertService governanceOpsAlertService) {
        this.governanceOpsAlertService = governanceOpsAlertService;
    }

    @EventListener
    public void onRaised(GovernanceOpsAlertRaisedEvent event) {
        if (event == null) {
            return;
        }
        governanceOpsAlertService.raiseOrRefresh(new GovernanceOpsAlertCommand(
                event.alertType(),
                event.alertCode(),
                event.subjectType(),
                event.subjectId(),
                event.productId(),
                event.riskMetricId(),
                event.releaseBatchId(),
                event.traceId(),
                event.deviceCode(),
                event.productKey(),
                event.severityLevel(),
                event.affectedCount(),
                event.alertTitle(),
                event.alertMessage(),
                event.dimensionKey(),
                event.dimensionLabel(),
                event.sourceStage(),
                event.snapshotJson(),
                defaultOperatorUserId(event.operatorUserId())
        ));
    }

    @EventListener
    public void onRecovered(GovernanceOpsAlertRecoveredEvent event) {
        if (event == null) {
            return;
        }
        governanceOpsAlertService.recover(
                event.alertType(),
                event.alertCode(),
                defaultOperatorUserId(event.operatorUserId()),
                event.recoveryComment()
        );
    }

    private Long defaultOperatorUserId(Long operatorUserId) {
        return operatorUserId == null || operatorUserId <= 0 ? SYSTEM_OPERATOR_ID : operatorUserId;
    }
}
