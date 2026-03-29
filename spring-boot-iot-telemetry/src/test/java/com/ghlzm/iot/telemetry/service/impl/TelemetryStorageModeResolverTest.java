package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryStorageModeResolverTest {

    @Test
    void shouldResolveV2PrimaryWriteAndLegacyMirrorFlags() {
        IotProperties properties = new IotProperties();
        properties.getTelemetry().setStorageType("tdengine");
        properties.getTelemetry().setPrimaryStorage("tdengine-v2");
        properties.getTelemetry().getLegacyMirror().setEnabled(true);
        properties.getTelemetry().getReadRouting().setLatestSource("v2");
        properties.getTelemetry().getReadRouting().setLegacyReadFallbackEnabled(true);

        TelemetryStorageModeResolver resolver = new TelemetryStorageModeResolver(properties);

        assertTrue(resolver.isTdengineEnabled());
        assertTrue(resolver.isV2PrimaryEnabled());
        assertTrue(resolver.isLegacyMirrorEnabled());
        assertTrue(resolver.isLegacyReadFallbackEnabled());
        assertEquals("v2", resolver.latestSource());
    }
}
