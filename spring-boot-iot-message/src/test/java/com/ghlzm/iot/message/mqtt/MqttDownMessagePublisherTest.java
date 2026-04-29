package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttDownMessagePublisherTest {

    @Mock
    private MqttMessageConsumer mqttMessageConsumer;
    @Mock
    private ProtocolAdapterRegistry protocolAdapterRegistry;
    @Mock
    private ProtocolAdapter protocolAdapter;

    @Test
    void publishRawIsolatedShouldDelegateToDedicatedPublisherClient() {
        MqttDownMessagePublisher publisher = new MqttDownMessagePublisher(
                mqttMessageConsumer,
                protocolAdapterRegistry,
                buildIotProperties()
        );

        byte[] payload = "plain-text".getBytes(StandardCharsets.UTF_8);
        publisher.publishRawIsolated("$dp", payload, 1, false);

        verify(mqttMessageConsumer).publishIsolated("$dp", payload, 1, false);
    }

    @Test
    void publishIsolatedShouldEncodeViaProtocolAdapterAndUseDedicatedPublisherClient() {
        MqttDownMessagePublisher publisher = new MqttDownMessagePublisher(
                mqttMessageConsumer,
                protocolAdapterRegistry,
                buildIotProperties()
        );
        DeviceDownMessage message = new DeviceDownMessage();
        ProtocolContext context = new ProtocolContext();
        byte[] payload = "encoded".getBytes(StandardCharsets.UTF_8);

        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.encode(eq(message), any(ProtocolContext.class))).thenReturn(payload);

        publisher.publishIsolated("mqtt-json", "/sys/demo/device/thing/property/set", message, context, 1, false);

        verify(mqttMessageConsumer).publishIsolated("/sys/demo/device/thing/property/set", payload, 1, false);
    }

    private IotProperties buildIotProperties() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Mqtt mqtt = new IotProperties.Mqtt();
        mqtt.setQos(1);
        iotProperties.setMqtt(mqtt);
        return iotProperties;
    }
}
