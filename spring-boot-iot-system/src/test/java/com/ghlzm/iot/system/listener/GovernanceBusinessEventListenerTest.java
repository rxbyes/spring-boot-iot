package com.ghlzm.iot.system.listener;

import com.ghlzm.iot.common.event.governance.ProductContractReleasedEvent;
import com.ghlzm.iot.common.event.governance.RiskMetricCatalogPublishedEvent;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GovernanceBusinessEventListenerTest {

    private static final class RecordingEvidenceRecorder implements ObservabilityEvidenceRecorder {
        private final AtomicReference<BusinessEventLogRecord> lastEvent = new AtomicReference<>();

        @Override
        public void recordBusinessEvent(BusinessEventLogRecord event) {
            lastEvent.set(event);
        }
    }

    private GovernanceBusinessEventListener listener;
    private RecordingEvidenceRecorder evidenceRecorder;

    @BeforeEach
    void setUp() {
        listener = new GovernanceBusinessEventListener();
        evidenceRecorder = new RecordingEvidenceRecorder();
        listener.setObservabilityEvidenceRecorder(evidenceRecorder);
    }

    @Test
    void onContractReleasedShouldRecordBusinessEvent() {
        listener.onContractReleased(new ProductContractReleasedEvent(
                1L,
                1001L,
                7001L,
                "phase1-crack",
                List.of("value", "sensor_state"),
                10001L,
                99001L
        ));

        BusinessEventLogRecord event = evidenceRecorder.lastEvent.get();
        assertNotNull(event);
        assertEquals("product.contract.released", event.getEventCode());
        assertEquals("contract_release_batch", event.getObjectType());
        assertEquals("7001", event.getObjectId());
        assertEquals(1001L, event.getMetadata().get("productId"));
        assertEquals(99001L, event.getMetadata().get("approvalOrderId"));
    }

    @Test
    void onRiskMetricCatalogPublishedShouldRecordBusinessEvent() {
        listener.onRiskMetricCatalogPublished(new RiskMetricCatalogPublishedEvent(
                1L,
                1001L,
                7001L,
                List.of(9001L, 9002L),
                List.of(8001L),
                10001L
        ));

        BusinessEventLogRecord event = evidenceRecorder.lastEvent.get();
        assertNotNull(event);
        assertEquals("risk.metric_catalog.published", event.getEventCode());
        assertEquals("risk_metric_catalog", event.getObjectType());
        assertEquals("7001", event.getObjectId());
        assertEquals(2, event.getMetadata().get("publishedRiskMetricCount"));
        assertEquals(1, event.getMetadata().get("retiredRiskMetricCount"));
    }
}
