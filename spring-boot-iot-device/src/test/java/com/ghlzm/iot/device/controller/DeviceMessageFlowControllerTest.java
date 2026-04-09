package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.MessageFlowRecentQuery;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowOpsOverviewVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowRecentSessionVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowSessionVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowTimelineVO;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowMetricsRecorder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSession;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStatuses;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimeline;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimelineStore;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceMessageFlowControllerTest {

    @Mock
    private MessageFlowTimelineStore messageFlowTimelineStore;
    @Mock
    private MessageFlowMetricsRecorder messageFlowMetricsRecorder;
    @Mock
    private DeviceMessageService deviceMessageService;
    @Mock
    private Authentication authentication;

    private DeviceMessageFlowController controller;

    @BeforeEach
    void setUp() {
        MessageFlowProperties properties = new MessageFlowProperties();
        properties.setSessionMatchWindowSeconds(120);
        properties.setRecentSessionLimit(500);
        controller = new DeviceMessageFlowController(
                messageFlowTimelineStore,
                messageFlowMetricsRecorder,
                properties,
                deviceMessageService
        );
    }

    @Test
    void getSessionShouldRecordLookupHitAndAttachTimeline() {
        MessageFlowSession session = new MessageFlowSession();
        session.setSessionId("session-001");
        session.setTransportMode("HTTP");
        session.setStatus(MessageFlowStatuses.SESSION_COMPLETED);
        session.setSubmittedAt(LocalDateTime.of(2026, 3, 23, 10, 0));
        session.setTraceId("trace-001");
        session.setDeviceCode("demo-device-01");
        session.setTopic("/message/http/report");
        session.setCorrelationPending(Boolean.FALSE);
        MessageFlowTimeline timeline = new MessageFlowTimeline();
        timeline.setTraceId("trace-001");
        timeline.setSessionId("session-001");
        timeline.setStatus(MessageFlowStatuses.SESSION_COMPLETED);

        when(messageFlowTimelineStore.getSession("session-001")).thenReturn(Optional.of(session));
        when(messageFlowTimelineStore.getTimeline("trace-001")).thenReturn(Optional.of(timeline));

        R<MessageFlowSessionVO> response = controller.getSession("session-001");

        assertNotNull(response.getData());
        assertEquals("session-001", response.getData().getSessionId());
        assertNotNull(response.getData().getTimeline());
        assertEquals("trace-001", response.getData().getTimeline().getTraceId());
        verify(messageFlowMetricsRecorder).recordLookup(
                MessageFlowMetricsRecorder.LOOKUP_TARGET_SESSION,
                MessageFlowMetricsRecorder.LOOKUP_RESULT_HIT
        );
    }

    @Test
    void getTraceShouldRecordLookupMissWhenTimelineAbsent() {
        when(messageFlowTimelineStore.getTimeline("trace-missing")).thenReturn(Optional.empty());

        R<MessageFlowTimelineVO> response = controller.getTrace("trace-missing");

        assertNull(response.getData());
        verify(messageFlowMetricsRecorder).recordLookup(
                MessageFlowMetricsRecorder.LOOKUP_TARGET_TRACE,
                MessageFlowMetricsRecorder.LOOKUP_RESULT_MISS
        );
    }

    @Test
    void getRecentSessionsShouldFilterAndPruneExpiredMembers() {
        MessageFlowSession session = new MessageFlowSession();
        session.setSessionId("session-002");
        session.setTransportMode("MQTT");
        session.setStatus(MessageFlowStatuses.SESSION_COMPLETED);
        session.setSubmittedAt(LocalDateTime.of(2026, 3, 23, 10, 5));
        session.setTraceId("trace-002");
        session.setDeviceCode("demo-device-01");
        session.setTopic("$dp");
        session.setCorrelationPending(Boolean.FALSE);
        MessageFlowTimeline timeline = new MessageFlowTimeline();
        timeline.setTraceId("trace-002");

        when(messageFlowTimelineStore.getRecentSessionIds(5)).thenReturn(List.of("session-expired", "session-002"));
        when(messageFlowTimelineStore.getSession("session-expired")).thenReturn(Optional.empty());
        when(messageFlowTimelineStore.getSession("session-002")).thenReturn(Optional.of(session));
        when(messageFlowTimelineStore.getTimeline("trace-002")).thenReturn(Optional.of(timeline));

        MessageFlowRecentQuery query = new MessageFlowRecentQuery();
        query.setTransportMode("MQTT");

        R<List<MessageFlowRecentSessionVO>> response = controller.getRecentSessions(query, 5);

        assertEquals(1, response.getData().size());
        assertEquals("session-002", response.getData().get(0).getSessionId());
        assertEquals(Boolean.TRUE, response.getData().get(0).getTimelineAvailable());
        verify(messageFlowTimelineStore).removeRecentSession("session-expired");
    }

    @Test
    void getOpsOverviewShouldMapSnapshotToVo() {
        MessageFlowMetricsRecorder.OverviewSnapshot snapshot = new MessageFlowMetricsRecorder.OverviewSnapshot(
                LocalDateTime.of(2026, 3, 23, 9, 30),
                List.of(new MessageFlowMetricsRecorder.SessionCountSnapshot("HTTP", "COMPLETED", 2L)),
                List.of(new MessageFlowMetricsRecorder.CorrelationCountSnapshot("matched", 1L)),
                List.of(new MessageFlowMetricsRecorder.LookupCountSnapshot("trace", "hit", 3L)),
                List.of(new MessageFlowMetricsRecorder.StageMetricSnapshot("INGRESS", 2L, 0L, 0L, 6D, 8D, 10L))
        );
        when(messageFlowMetricsRecorder.snapshot()).thenReturn(snapshot);

        R<MessageFlowOpsOverviewVO> response = controller.getOpsOverview();

        assertNotNull(response.getData());
        assertEquals(1, response.getData().getSessionCounts().size());
        assertEquals(1, response.getData().getCorrelationCounts().size());
        assertEquals(1, response.getData().getLookupCounts().size());
        assertEquals(1, response.getData().getStageMetrics().size());
        assertEquals("INGRESS", response.getData().getStageMetrics().get(0).getStage());
    }

    @Test
    void getTraceDetailShouldReturnRecoveredPayloadComparisonWhenTimelineMissing() {
        MessageTraceDetailVO detail = new MessageTraceDetailVO();
        detail.setId(1L);
        detail.setTraceId("trace-001");
        detail.setDeviceCode("demo-device-01");
        detail.setProductKey("demo-product");
        detail.setTopic("$dp");
        detail.setRawPayload("{\"header\":{\"appId\":\"62000001\"},\"bodies\":{\"body\":\"cipher-text\"}}");
        detail.setDecryptedPayload("{\"17165802\":{\"temperature\":26.5}}");
        detail.setDecodedPayload(Map.of(
                "messageType", "property",
                "deviceCode", "17165802",
                "properties", Map.of("temperature", 26.5)
        ));
        when(authentication.getPrincipal()).thenReturn(new JwtUserPrincipal(99L, "tester"));
        when(deviceMessageService.getMessageTraceDetail(99L, 1L)).thenReturn(detail);
        when(messageFlowTimelineStore.getTimeline("trace-001")).thenReturn(Optional.empty());

        R<MessageTraceDetailVO> response = controller.getTraceDetail(1L, authentication);

        assertNotNull(response.getData());
        assertEquals("{\"17165802\":{\"temperature\":26.5}}", response.getData().getDecryptedPayload());
        assertEquals("17165802", response.getData().getDecodedPayload().get("deviceCode"));
        verify(deviceMessageService).getMessageTraceDetail(99L, 1L);
    }
}
