package com.ghlzm.iot.admin.health;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttConsumerRuntimeState;
import com.ghlzm.iot.message.mqtt.MqttMessageConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MqttConsumerHealthIndicatorTest {

    @Test
    void healthShouldExposeMqttConsumerSnapshot() {
        IotProperties properties = new IotProperties();
        IotProperties.Mqtt mqtt = new IotProperties.Mqtt();
        mqtt.setEnabled(true);
        mqtt.setBrokerUrl("tcp://admin:secret@iot.ghatg.com:1883");
        properties.setMqtt(mqtt);

        MqttConsumerRuntimeState runtimeState = new MqttConsumerRuntimeState();
        runtimeState.markConnected();
        runtimeState.markSubscribed(List.of("$dp"));
        runtimeState.markMessageReceived();
        runtimeState.markFailure("device_validate", "trace-001");

        MqttMessageConsumer consumer = mock(MqttMessageConsumer.class);
        when(consumer.isRunning()).thenReturn(true);
        when(consumer.isConsumerActive()).thenReturn(true);
        when(consumer.isConnected()).thenReturn(true);
        when(consumer.isClusterSingletonEnabled()).thenReturn(false);
        when(consumer.isLeader()).thenReturn(true);
        when(consumer.getLeadershipMode()).thenReturn("SINGLE");
        when(consumer.getEffectiveClientId()).thenReturn("shared-client-dev-01");

        MqttConsumerHealthIndicator indicator = new MqttConsumerHealthIndicator(properties, consumer, runtimeState);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals(true, health.getDetails().get("running"));
        assertEquals(true, health.getDetails().get("consumerActive"));
        assertEquals(true, health.getDetails().get("connected"));
        assertEquals("shared-client-dev-01", health.getDetails().get("clientId"));
        assertEquals(List.of("$dp"), health.getDetails().get("subscribeTopics"));
        assertTrue(String.valueOf(health.getDetails().get("brokerUrl")).contains("***@iot.ghatg.com:1883"));
        assertEquals("device_validate", health.getDetails().get("lastFailureStage"));
        assertEquals("trace-001", health.getDetails().get("lastFailureTraceId"));
    }

    @Test
    void healthShouldTreatStandbyNodeAsUp() {
        IotProperties properties = new IotProperties();
        properties.getMqtt().setEnabled(true);
        properties.getMqtt().setBrokerUrl("tcp://iot.ghatg.com:1883");

        MqttConsumerRuntimeState runtimeState = new MqttConsumerRuntimeState();
        MqttMessageConsumer consumer = mock(MqttMessageConsumer.class);
        when(consumer.isRunning()).thenReturn(true);
        when(consumer.isConsumerActive()).thenReturn(false);
        when(consumer.isConnected()).thenReturn(false);
        when(consumer.isClusterSingletonEnabled()).thenReturn(true);
        when(consumer.isLeader()).thenReturn(false);
        when(consumer.getLeadershipMode()).thenReturn("STANDBY");
        when(consumer.getCurrentLeaderOwnerId()).thenReturn(java.util.Optional.of("leader-node"));

        MqttConsumerHealthIndicator indicator = new MqttConsumerHealthIndicator(properties, consumer, runtimeState);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals("STANDBY", health.getDetails().get("leadershipMode"));
        assertEquals("leader-node", health.getDetails().get("clusterLeaderOwnerId"));
    }
}
