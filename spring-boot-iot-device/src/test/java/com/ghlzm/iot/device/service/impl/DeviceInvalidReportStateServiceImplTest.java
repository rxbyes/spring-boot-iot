package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.DeviceInvalidReportState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceInvalidReportStateServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private DeviceInvalidReportStateSchemaSupport schemaSupport;

    private DeviceInvalidReportStateServiceImpl service;

    @BeforeEach
    void setUp() {
        when(schemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of(
                "id", "governance_key", "reason_code", "device_code", "product_key",
                "failure_stage", "topic", "last_trace_id", "last_payload",
                "first_seen_time", "last_seen_time", "hit_count", "sampled_count",
                "suppressed_count", "suppressed_until", "resolved", "resolved_time", "deleted"
        )));
        service = new DeviceInvalidReportStateServiceImpl(jdbcTemplate, schemaSupport);
    }

    @Test
    void upsertStateShouldInsertOnFirstHitAndCarryCounters() {
        DeviceInvalidReportState state = new DeviceInvalidReportState();
        state.setGovernanceKey("tenant=1|product=obs-product|device=missing-01|reason=DEVICE_NOT_FOUND");
        state.setReasonCode("DEVICE_NOT_FOUND");
        state.setDeviceCode("missing-01");
        state.setProductKey("obs-product");
        state.setFailureStage("device_validate");
        state.setLastTraceId("trace-missing-001");
        state.setLastPayload("{\"deviceCode\":\"missing-01\"}");
        state.setHitCount(1L);

        service.upsertState(state);

        verify(jdbcTemplate).update(startsWith("INSERT INTO iot_device_invalid_report_state"), any(Object[].class));
    }

    @Test
    void markResolvedByDeviceShouldResolveAllMatchingOpenStates() {
        service.markResolvedByDevice("obs-product", "missing-01", LocalDateTime.of(2026, 3, 27, 22, 20, 0));

        verify(jdbcTemplate).update(
                contains("SET resolved = 1"),
                eq(LocalDateTime.of(2026, 3, 27, 22, 20, 0)),
                eq("obs-product"),
                eq("missing-01")
        );
    }

    @Test
    void upsertStateShouldAccumulateCountersWhenRecordExists() {
        when(jdbcTemplate.update(startsWith("UPDATE iot_device_invalid_report_state"), any(Object[].class))).thenReturn(1);

        DeviceInvalidReportState state = new DeviceInvalidReportState();
        state.setGovernanceKey("tenant=1|product=obs-product|device=missing-01|reason=DEVICE_NOT_FOUND");
        state.setHitCount(1L);
        state.setSampledCount(1L);
        state.setSuppressedCount(0L);

        service.upsertState(state);

        verify(jdbcTemplate).update(contains("hit_count = COALESCE(hit_count, 0) + ?"), any(Object[].class));
    }
}
