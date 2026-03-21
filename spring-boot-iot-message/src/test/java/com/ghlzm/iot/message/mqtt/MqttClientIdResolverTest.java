package com.ghlzm.iot.message.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqttClientIdResolverTest {

    @Test
    void shouldAppendRuntimeSuffixForDefaultSharedClientId() {
        String resolved = MqttClientIdResolver.resolve(MqttClientIdResolver.DEFAULT_SHARED_CLIENT_ID);

        assertNotEquals(MqttClientIdResolver.DEFAULT_SHARED_CLIENT_ID, resolved);
        assertTrue(resolved.startsWith(MqttClientIdResolver.DEFAULT_SHARED_CLIENT_ID + "-"));
        assertTrue(resolved.length() <= 64);
    }

    @Test
    void shouldKeepExplicitClientIdUnchanged() {
        assertEquals("custom-mqtt-client", MqttClientIdResolver.resolve("custom-mqtt-client"));
    }

    @Test
    void shouldUseDerivedIdWhenConfiguredClientIdBlank() {
        String resolved = MqttClientIdResolver.resolve("   ");

        assertTrue(resolved.startsWith(MqttClientIdResolver.DEFAULT_SHARED_CLIENT_ID + "-"));
    }
}
