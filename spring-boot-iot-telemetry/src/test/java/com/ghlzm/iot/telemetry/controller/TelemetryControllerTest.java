package com.ghlzm.iot.telemetry.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.telemetry.service.TelemetryHistoryMigrationService;
import com.ghlzm.iot.telemetry.service.TelemetryQueryService;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchResponse;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryControllerTest {

    @Test
    void historyBatchShouldDelegateToQueryService() {
        TelemetryQueryService telemetryQueryService = mock(TelemetryQueryService.class);
        TelemetryHistoryMigrationService telemetryHistoryMigrationService = mock(TelemetryHistoryMigrationService.class);
        TelemetryController controller = new TelemetryController(telemetryQueryService, telemetryHistoryMigrationService);
        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1", "S1_ZT_1.sensor_state.L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");
        TelemetryHistoryBatchResponse payload = new TelemetryHistoryBatchResponse();
        payload.setDeviceId(2001L);
        payload.setRangeCode("7d");
        when(telemetryQueryService.getHistoryBatch(request)).thenReturn(payload);

        R<TelemetryHistoryBatchResponse> response = controller.historyBatch(request);

        assertEquals(payload, response.getData());
        verify(telemetryQueryService).getHistoryBatch(request);
    }

    @Test
    void latestShouldDelegateToQueryService() {
        TelemetryQueryService telemetryQueryService = mock(TelemetryQueryService.class);
        TelemetryHistoryMigrationService telemetryHistoryMigrationService = mock(TelemetryHistoryMigrationService.class);
        TelemetryController controller = new TelemetryController(telemetryQueryService, telemetryHistoryMigrationService);
        Map<String, Object> payload = Map.of(
                "deviceId", 2001L,
                "deviceCode", "demo-device-01",
                "storageType", "tdengine"
        );
        when(telemetryQueryService.getLatest(2001L)).thenReturn(payload);

        R<Map<String, Object>> response = controller.latest(2001L);

        assertEquals(payload, response.getData());
        verify(telemetryQueryService).getLatest(2001L);
    }

    @Test
    void migrateHistoryShouldDelegateToMigrationService() {
        TelemetryQueryService telemetryQueryService = mock(TelemetryQueryService.class);
        TelemetryHistoryMigrationService telemetryHistoryMigrationService = mock(TelemetryHistoryMigrationService.class);
        TelemetryController controller = new TelemetryController(telemetryQueryService, telemetryHistoryMigrationService);
        TelemetryHistoryMigrationRequest request = new TelemetryHistoryMigrationRequest();
        request.setDeviceId(2001L);
        request.setBatchSize(300);
        TelemetryHistoryMigrationResult payload = new TelemetryHistoryMigrationResult();
        payload.setSource("normalized");
        payload.setMigratedDeviceCount(1);
        payload.setScannedPointCount(12);
        payload.setWrittenPointCount(12);
        payload.setLatestProjectedPointCount(12);
        when(telemetryHistoryMigrationService.migrate(request)).thenReturn(payload);

        R<TelemetryHistoryMigrationResult> response = controller.migrateHistory(request);

        assertEquals(payload, response.getData());
        verify(telemetryHistoryMigrationService).migrate(request);
    }
}
