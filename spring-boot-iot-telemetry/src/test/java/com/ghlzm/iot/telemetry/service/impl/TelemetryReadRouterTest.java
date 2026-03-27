package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryReadRouterTest {

    @Test
    void shouldResolveReadSourcesAndLegacyFallback() {
        IotProperties properties = new IotProperties();
        properties.getTelemetry().getReadRouting().setLatestSource("v2");
        properties.getTelemetry().getReadRouting().setHistorySource("legacy");
        properties.getTelemetry().getReadRouting().setAggregateSource("v2-aggregate");
        properties.getTelemetry().getReadRouting().setLegacyReadFallbackEnabled(true);

        TelemetryReadRouter router = new TelemetryReadRouter(properties);

        assertEquals("v2", router.latestSource());
        assertEquals("legacy", router.historySource());
        assertEquals("v2-aggregate", router.aggregateSource());
        assertTrue(router.isLegacyReadFallbackEnabled());
    }
}
