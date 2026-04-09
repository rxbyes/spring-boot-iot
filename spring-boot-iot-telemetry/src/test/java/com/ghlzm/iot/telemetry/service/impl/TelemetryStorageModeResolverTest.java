package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryStorageModeResolverTest {

    @Test
    void shouldResolveV2PrimaryWriteAndLegacyMirrorFlags() {
        IotProperties properties = new IotProperties();
        properties.getTelemetry().setStorageType("tdengine");
        properties.getTelemetry().setPrimaryStorage("tdengine-v2");
        properties.getTelemetry().getLegacyMirror().setEnabled(true);
        properties.getTelemetry().getAggregate().setEnabled(true);
        properties.getTelemetry().getAggregate().setHourlyEnabled(true);
        properties.getTelemetry().getColdArchive().setEnabled(true);
        properties.getTelemetry().getReadRouting().setLatestSource("v2");
        properties.getTelemetry().getReadRouting().setLegacyReadFallbackEnabled(true);

        TelemetryStorageModeResolver resolver = new TelemetryStorageModeResolver(properties);

        assertTrue(resolver.isTdengineEnabled());
        assertTrue(resolver.isV2PrimaryEnabled());
        assertTrue(resolver.isLegacyMirrorEnabled());
        assertTrue(resolver.isAggregateEnabled());
        assertTrue(resolver.isAggregateHourlyEnabled());
        assertTrue(resolver.isColdArchiveEnabled());
        assertTrue(resolver.isLegacyReadFallbackEnabled());
        assertEquals("v2", resolver.latestSource());
    }

    @Test
    void shouldRequireHourlySwitchBeforeDispatchingAggregate() {
        IotProperties properties = new IotProperties();
        properties.getTelemetry().setStorageType("tdengine");
        properties.getTelemetry().getAggregate().setEnabled(true);
        properties.getTelemetry().getAggregate().setHourlyEnabled(false);

        TelemetryStorageModeResolver resolver = new TelemetryStorageModeResolver(properties);

        assertTrue(resolver.isAggregateEnabled());
        assertFalse(resolver.isAggregateHourlyEnabled());
    }
}
