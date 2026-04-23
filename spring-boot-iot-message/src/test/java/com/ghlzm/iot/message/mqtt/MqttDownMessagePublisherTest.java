package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MqttDownMessagePublisherTest {

    @Mock
    private MqttMessageConsumer mqttMessageConsumer;
    @Mock
    private ProtocolAdapterRegistry protocolAdapterRegistry;

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

    private IotProperties buildIotProperties() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Mqtt mqtt = new IotProperties.Mqtt();
        mqtt.setQos(1);
        iotProperties.setMqtt(mqtt);
        return iotProperties;
    }
}
