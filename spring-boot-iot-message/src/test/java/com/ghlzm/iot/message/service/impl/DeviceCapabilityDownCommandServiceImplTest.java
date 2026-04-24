package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.device.capability.DeviceCapabilityDefinition;
import com.ghlzm.iot.device.capability.DeviceCapabilityRegistry;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadata;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadataParser;
import com.ghlzm.iot.device.capability.WarningDeviceKind;
import com.ghlzm.iot.device.capability.VideoDeviceKind;
import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttDownMessagePublisher;
import com.ghlzm.iot.message.service.capability.WarningCapabilityPayloadBuilder;
import com.ghlzm.iot.message.service.capability.VideoCapabilityPayloadBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCapabilityDownCommandServiceImplTest {

    @Mock
    private MqttDownMessagePublisher mqttDownMessagePublisher;
    @Mock
    private CommandRecordService commandRecordService;

    @Test
    void executeShouldCreateRecordAndPublishBroadcastPayload() {
        WarningCapabilityPayloadBuilder warningBuilder = new WarningCapabilityPayloadBuilder();
        VideoCapabilityPayloadBuilder videoBuilder = new VideoCapabilityPayloadBuilder();
        DeviceCapabilityDownCommandServiceImpl service = new DeviceCapabilityDownCommandServiceImpl(
                mqttDownMessagePublisher,
                commandRecordService,
                buildIotProperties(),
                warningBuilder,
                videoBuilder
        );

        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(8L);
        device.setDeviceCode("demo-device-01");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("warning-product");
        product.setMetadataJson("""
                {"governance":{"productCapabilityType":"WARNING","warningDeviceKind":"BROADCAST"}}
                """);

        ProductCapabilityMetadata metadata = new ProductCapabilityMetadataParser().parse(product.getMetadataJson());
        DeviceCapabilityDefinition capability = new DeviceCapabilityRegistry().resolve(metadata).stream()
                .filter(item -> "broadcast_play".equals(item.code()))
                .findFirst()
                .orElseThrow();

        DeviceCapabilityCommandRequest request = new DeviceCapabilityCommandRequest();
        request.setDevice(device);
        request.setProduct(product);
        request.setMetadata(metadata);
        request.setCapability(capability);
        request.setParams(Map.of("content", "road-work", "bNum", 1, "volume", 80));

        doAnswer(invocation -> {
            CommandRecord record = invocation.getArgument(0);
            record.setId(9001L);
            return record;
        }).when(commandRecordService).create(any(CommandRecord.class));

        service.execute(request);

        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mqttDownMessagePublisher).publishRaw(
                org.mockito.ArgumentMatchers.eq("/iot/broadcast/demo-device-01"),
                payloadCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(false)
        );
        assertEquals("$cmd=broadcast&b_num=1&b_size=9&b_content=road-work&volume=80&msgid=177", new String(payloadCaptor.getValue(), StandardCharsets.UTF_8).replaceFirst("msgid=\\d+", "msgid=177"));
        verify(commandRecordService).markSent(eq(9001L), any(LocalDateTime.class));
    }

    private IotProperties buildIotProperties() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Mqtt mqtt = new IotProperties.Mqtt();
        mqtt.setQos(1);
        iotProperties.setMqtt(mqtt);
        return iotProperties;
    }
}
