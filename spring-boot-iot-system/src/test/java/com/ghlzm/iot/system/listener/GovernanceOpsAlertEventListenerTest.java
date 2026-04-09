package com.ghlzm.iot.system.listener;

import com.ghlzm.iot.common.event.governance.GovernanceOpsAlertRaisedEvent;
import com.ghlzm.iot.common.event.governance.GovernanceOpsAlertRecoveredEvent;
import com.ghlzm.iot.system.service.GovernanceOpsAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GovernanceOpsAlertEventListenerTest {

    @Mock
    private GovernanceOpsAlertService governanceOpsAlertService;

    private GovernanceOpsAlertEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new GovernanceOpsAlertEventListener(governanceOpsAlertService);
    }

    @Test
    void onRaisedShouldProjectEventIntoPersistentAlertCommand() {
        listener.onRaised(new GovernanceOpsAlertRaisedEvent(
                1L,
                "FIELD_DRIFT",
                "product:1001:field_drift",
                "PRODUCT",
                1001L,
                1001L,
                null,
                null,
                null,
                null,
                null,
                "WARN",
                2L,
                "字段漂移告警",
                "value 已偏离正式合同",
                "product:1001:value",
                "产品1001/value",
                "PAYLOAD_APPLY",
                "{}",
                10001L
        ));

        verify(governanceOpsAlertService).raiseOrRefresh(argThat(command ->
                "FIELD_DRIFT".equals(command.alertType())
                        && "product:1001:field_drift".equals(command.alertCode())
                        && "PRODUCT".equals(command.subjectType())
                        && Long.valueOf(1001L).equals(command.subjectId())
                        && Long.valueOf(1001L).equals(command.productId())
                        && "WARN".equals(command.severityLevel())
                        && Long.valueOf(2L).equals(command.affectedCount())
                        && "字段漂移告警".equals(command.alertTitle())
                        && "{}".equals(command.snapshotJson())
                        && Long.valueOf(10001L).equals(command.operatorUserId())
        ));
    }

    @Test
    void onRecoveredShouldResolvePersistentAlert() {
        listener.onRecovered(new GovernanceOpsAlertRecoveredEvent(
                1L,
                "FIELD_DRIFT",
                "product:1001:field_drift",
                "field drift recovered",
                10001L
        ));

        verify(governanceOpsAlertService).recover(
                "FIELD_DRIFT",
                "product:1001:field_drift",
                10001L,
                "field drift recovered"
        );
    }
}
