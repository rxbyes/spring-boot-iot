package com.ghlzm.iot.system.listener;

import com.ghlzm.iot.common.event.governance.RiskMetricCatalogPublishedEvent;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GovernanceWorkItemEventListenerTest {

    @Mock
    private GovernanceWorkItemService governanceWorkItemService;

    private GovernanceWorkItemEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new GovernanceWorkItemEventListener(governanceWorkItemService);
    }

    @Test
    void onCatalogPublishedShouldResolveContractReleaseAndOpenRiskBindingWorkItem() {
        listener.onCatalogPublished(new RiskMetricCatalogPublishedEvent(
                1L,
                1001L,
                7001L,
                List.of(9001L),
                List.of(),
                10001L
        ));

        verify(governanceWorkItemService).resolve(
                "PENDING_CONTRACT_RELEASE",
                "PRODUCT",
                1001L,
                1L,
                "目录已发布"
        );
        verify(governanceWorkItemService).openOrRefresh(argThat(command ->
                "PENDING_RISK_BINDING".equals(command.workItemCode())
                        && "PRODUCT".equals(command.subjectType())
                        && Long.valueOf(1001L).equals(command.subjectId())
                        && Long.valueOf(1001L).equals(command.productId())
                        && Long.valueOf(7001L).equals(command.releaseBatchId())
                        && "RISK_BINDING".equals(command.sourceStage())
                        && "目录已发布，待完成风险点绑定".equals(command.blockingReason())
                        && "P1".equals(command.priorityLevel())
                        && Long.valueOf(1L).equals(command.operatorUserId())
                        && command.snapshotJson() != null
                        && command.snapshotJson().contains("publishedRiskMetricIds")
        ));
    }

    @Test
    void onCatalogPublishedShouldIgnoreEventWithoutProductId() {
        listener.onCatalogPublished(new RiskMetricCatalogPublishedEvent(
                1L,
                null,
                7001L,
                List.of(9001L),
                List.of(),
                10001L
        ));

        verifyNoInteractions(governanceWorkItemService);
    }
}
