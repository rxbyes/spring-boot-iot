package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcObservabilityEvidenceRecorderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcObservabilityEvidenceRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new JdbcObservabilityEvidenceRecorder(jdbcTemplate);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(Object[].class)))
                .thenReturn(mockColumns());
        lenient().when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    }

    @Test
    void recordSpanShouldSanitizeAndBoundTagsJson() {
        ObservabilitySpanLogRecord span = new ObservabilitySpanLogRecord();
        span.setTraceId("trace-1");
        span.setSpanType("HTTP_REQUEST");
        span.setSpanName("HTTP_REQUEST");
        span.setTags(Map.of(
                "apiKey", "secret-demo",
                "payload", "x".repeat(400)
        ));

        recorder.recordSpan(span);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), argsCaptor.capture());
        String storedJson = findJsonArg(argsCaptor.getValue(), "\"apiKey\"");
        assertNotNull(storedJson);
        assertTrue(storedJson.contains("\"apiKey\":\"***\""));
        assertTrue(storedJson.contains("...(truncated)"));
    }

    @Test
    void recordBusinessEventShouldSanitizeAndBoundMetadataJson() {
        BusinessEventLogRecord event = new BusinessEventLogRecord();
        event.setTraceId("trace-event-1");
        event.setEventCode("governance.publish");
        event.setEventName("治理发布");
        event.setMetadata(Map.of(
                "merchantKey", "merchant-secret",
                "payload", "y".repeat(400)
        ));

        recorder.recordBusinessEvent(event);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), argsCaptor.capture());
        String storedJson = findJsonArg(argsCaptor.getValue(), "\"merchantKey\"");
        assertNotNull(storedJson);
        assertTrue(storedJson.contains("\"merchantKey\":\"***\""));
        assertTrue(storedJson.contains("...(truncated)"));
    }

    private List<String> mockColumns() {
        Set<String> columns = new LinkedHashSet<>();
        columns.add("id");
        columns.add("tenant_id");
        columns.add("trace_id");
        columns.add("span_type");
        columns.add("span_name");
        columns.add("domain_code");
        columns.add("event_code");
        columns.add("object_type");
        columns.add("object_id");
        columns.add("transport_type");
        columns.add("status");
        columns.add("duration_ms");
        columns.add("started_at");
        columns.add("finished_at");
        columns.add("error_class");
        columns.add("error_message");
        columns.add("tags_json");
        columns.add("event_name");
        columns.add("action_code");
        columns.add("request_method");
        columns.add("request_uri");
        columns.add("metadata_json");
        columns.add("occurred_at");
        columns.add("create_time");
        columns.add("deleted");
        columns.add("result_status");
        columns.add("source_type");
        return List.copyOf(columns);
    }

    private String findJsonArg(Object[] args, String keyFragment) {
        for (Object arg : args) {
            if (arg instanceof String text && text.contains(keyFragment)) {
                return text;
            }
        }
        return null;
    }
}
