package com.ghlzm.iot.framework.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IotPropertiesAlertingBindingTest {

    @Test
    void shouldExposeAlertingDefaults() {
        IotProperties properties = new IotProperties();
        IotProperties.Observability.Alerting alerting = properties.getObservability().getAlerting();

        assertFalse(alerting.getEnabled());
        assertEquals("observability_alert", alerting.getScene());
        assertEquals(60, alerting.getEvaluateIntervalSeconds());
        assertEquals(30, alerting.getCooldownMinutes());
        assertTrue(alerting.getSystemError().getEnabled());
        assertEquals(10, alerting.getSystemError().getWindowMinutes());
        assertEquals(5, alerting.getSystemError().getThreshold());
        assertTrue(alerting.getMqttDisconnect().getEnabled());
        assertEquals(5, alerting.getMqttDisconnect().getDurationMinutes());
        assertTrue(alerting.getFailureStage().getEnabled());
        assertEquals(10, alerting.getFailureStage().getWindowMinutes());
        assertEquals(10, alerting.getFailureStage().getThreshold());
        assertTrue(alerting.getInAppBridge().getEnabled());
        assertEquals(10, alerting.getInAppBridge().getWindowMinutes());
        assertEquals(3, alerting.getInAppBridge().getThreshold());
        assertTrue(alerting.getRiskGovernanceMissingPolicy().getEnabled());
        assertEquals(3, alerting.getRiskGovernanceMissingPolicy().getThreshold());
    }

    @Test
    void shouldBindAlertingProperties() {
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.ofEntries(
                Map.entry("iot.observability.alerting.enabled", "true"),
                Map.entry("iot.observability.alerting.scene", "ops_scene"),
                Map.entry("iot.observability.alerting.evaluate-interval-seconds", "120"),
                Map.entry("iot.observability.alerting.cooldown-minutes", "45"),
                Map.entry("iot.observability.alerting.system-error.enabled", "false"),
                Map.entry("iot.observability.alerting.system-error.window-minutes", "15"),
                Map.entry("iot.observability.alerting.system-error.threshold", "8"),
                Map.entry("iot.observability.alerting.mqtt-disconnect.enabled", "false"),
                Map.entry("iot.observability.alerting.mqtt-disconnect.duration-minutes", "7"),
                Map.entry("iot.observability.alerting.failure-stage.enabled", "false"),
                Map.entry("iot.observability.alerting.failure-stage.window-minutes", "20"),
                Map.entry("iot.observability.alerting.failure-stage.threshold", "12"),
                Map.entry("iot.observability.alerting.in-app-bridge.enabled", "false"),
                Map.entry("iot.observability.alerting.in-app-bridge.window-minutes", "25"),
                Map.entry("iot.observability.alerting.in-app-bridge.threshold", "6"),
                Map.entry("iot.observability.alerting.risk-governance-missing-policy.enabled", "false"),
                Map.entry("iot.observability.alerting.risk-governance-missing-policy.threshold", "9")
        )));

        IotProperties properties = binder.bind("iot", Bindable.of(IotProperties.class))
                .orElseThrow(() -> new IllegalStateException("iot properties should bind"));
        IotProperties.Observability.Alerting alerting = properties.getObservability().getAlerting();

        assertTrue(alerting.getEnabled());
        assertEquals("ops_scene", alerting.getScene());
        assertEquals(120, alerting.getEvaluateIntervalSeconds());
        assertEquals(45, alerting.getCooldownMinutes());
        assertFalse(alerting.getSystemError().getEnabled());
        assertEquals(15, alerting.getSystemError().getWindowMinutes());
        assertEquals(8, alerting.getSystemError().getThreshold());
        assertFalse(alerting.getMqttDisconnect().getEnabled());
        assertEquals(7, alerting.getMqttDisconnect().getDurationMinutes());
        assertFalse(alerting.getFailureStage().getEnabled());
        assertEquals(20, alerting.getFailureStage().getWindowMinutes());
        assertEquals(12, alerting.getFailureStage().getThreshold());
        assertFalse(alerting.getInAppBridge().getEnabled());
        assertEquals(25, alerting.getInAppBridge().getWindowMinutes());
        assertEquals(6, alerting.getInAppBridge().getThreshold());
        assertFalse(alerting.getRiskGovernanceMissingPolicy().getEnabled());
        assertEquals(9, alerting.getRiskGovernanceMissingPolicy().getThreshold());
    }
}
