package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelemetryLatestProjectorTest {

    @Mock
    private TelemetryLatestProjectionRepository repository;

    private TelemetryLatestProjector projector;

    @BeforeEach
    void setUp() {
        projector = new TelemetryLatestProjector(repository);
    }

    @Test
    void shouldUpsertLatestSnapshotUsingTenantDeviceMetricKey() {
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(TelemetryProjectionTask.ProjectionType.LATEST);
        task.setPoints(List.of(
                point("temperature", 26.5D, LocalDateTime.of(2026, 3, 27, 9, 0)),
                point("temperature", 27.2D, LocalDateTime.of(2026, 3, 27, 9, 5)),
                point("humidity", 68D, LocalDateTime.of(2026, 3, 27, 9, 1))
        ));

        projector.project(task);

        ArgumentCaptor<List<TelemetryV2Point>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).bulkUpsert(captor.capture());
        List<TelemetryV2Point> points = captor.getValue();
        assertEquals(2, points.size());
        assertEquals("temperature", points.get(0).getMetricId());
        assertEquals(27.2D, points.get(0).getValueDouble());
    }

    private TelemetryV2Point point(String metricId, Double value, LocalDateTime reportedAt) {
        TelemetryV2Point point = new TelemetryV2Point();
        point.setStreamKind(TelemetryStreamKind.MEASURE);
        point.setTenantId(1L);
        point.setDeviceId(2001L);
        point.setProductId(1001L);
        point.setMetricId(metricId);
        point.setMetricCode(metricId);
        point.setMetricName(metricId);
        point.setValueType("double");
        point.setValueDouble(value);
        point.setReportedAt(reportedAt);
        point.setTraceId("trace-" + metricId);
        return point;
    }
}
