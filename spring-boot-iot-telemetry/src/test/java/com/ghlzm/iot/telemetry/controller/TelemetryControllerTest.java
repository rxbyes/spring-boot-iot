package com.ghlzm.iot.telemetry.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.telemetry.service.TelemetryQueryService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryControllerTest {

    @Test
    void latestShouldDelegateToQueryService() {
        TelemetryQueryService telemetryQueryService = mock(TelemetryQueryService.class);
        TelemetryController controller = new TelemetryController(telemetryQueryService);
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
}
