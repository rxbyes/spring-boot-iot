package com.ghlzm.iot.admin.health;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttConsumerRuntimeState;
import com.ghlzm.iot.message.mqtt.MqttMessageConsumer;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MQTT 消费端健康检查。
 */
@Component
public class MqttConsumerHealthIndicator implements HealthIndicator {

    private final IotProperties iotProperties;
    private final MqttMessageConsumer mqttMessageConsumer;
    private final MqttConsumerRuntimeState mqttConsumerRuntimeState;

    public MqttConsumerHealthIndicator(IotProperties iotProperties,
                                       MqttMessageConsumer mqttMessageConsumer,
                                       MqttConsumerRuntimeState mqttConsumerRuntimeState) {
        this.iotProperties = iotProperties;
        this.mqttMessageConsumer = mqttMessageConsumer;
        this.mqttConsumerRuntimeState = mqttConsumerRuntimeState;
    }

    @Override
    public Health health() {
        boolean enabled = iotProperties.getMqtt() != null && Boolean.TRUE.equals(iotProperties.getMqtt().getEnabled());
        boolean connected = mqttMessageConsumer.isConnected();
        MqttConsumerRuntimeState.Snapshot snapshot = mqttConsumerRuntimeState.snapshot();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("running", mqttMessageConsumer.isRunning());
        details.put("connected", connected);
        details.put("clientId", mqttMessageConsumer.getEffectiveClientId());
        details.put("brokerUrl", maskBrokerUrl(iotProperties.getMqtt() == null ? null : iotProperties.getMqtt().getBrokerUrl()));
        details.put("subscribeTopics", snapshot.subscribeTopics());
        details.put("lastConnectAt", snapshot.lastConnectAt());
        details.put("lastDisconnectAt", snapshot.lastDisconnectAt());
        details.put("lastMessageAt", snapshot.lastMessageAt());
        details.put("lastDispatchSuccessAt", snapshot.lastDispatchSuccessAt());
        details.put("lastFailureAt", snapshot.lastFailureAt());
        details.put("lastFailureStage", snapshot.lastFailureStage());
        details.put("lastFailureTraceId", snapshot.lastFailureTraceId());

        if (!enabled) {
            return Health.unknown().withDetails(details).build();
        }
        return connected ? Health.up().withDetails(details).build() : Health.down().withDetails(details).build();
    }

    private String maskBrokerUrl(String brokerUrl) {
        if (brokerUrl == null || brokerUrl.isBlank()) {
            return brokerUrl;
        }
        int schemeIndex = brokerUrl.indexOf("://");
        int atIndex = brokerUrl.indexOf('@');
        if (schemeIndex < 0 || atIndex < 0 || atIndex <= schemeIndex + 3) {
            return brokerUrl;
        }
        return brokerUrl.substring(0, schemeIndex + 3) + "***" + brokerUrl.substring(atIndex);
    }
}
