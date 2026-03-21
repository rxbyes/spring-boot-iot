package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttDownMessagePublisher;
import com.ghlzm.iot.message.mqtt.MqttMessageConsumer;
import com.ghlzm.iot.message.service.model.MqttReportPublishCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttReportPublishServiceImplTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private ProductService productService;
    @Mock
    private MqttMessageConsumer mqttMessageConsumer;
    @Mock
    private MqttDownMessagePublisher mqttDownMessagePublisher;

    @Test
    void publishShouldRejectWhenMqttClientDisconnected() {
        when(mqttMessageConsumer.isConnected()).thenReturn(false);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties()
        );

        BizException ex = assertThrows(BizException.class, () -> service.publish(buildCommand("plain-text")));
        assertEquals("MQTT 客户端未连接，无法执行模拟上报", ex.getMessage());
        verify(mqttMessageConsumer).isConnected();
        verifyNoInteractions(deviceService, productService, mqttDownMessagePublisher);
    }

    @Test
    void publishShouldRejectWhenProductMismatched() {
        Device device = buildDevice();
        Product product = buildProduct();
        product.setProductKey("other-product");

        when(mqttMessageConsumer.isConnected()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties()
        );

        BizException ex = assertThrows(BizException.class, () -> service.publish(buildCommand("plain-text")));
        assertEquals("模拟上报 productKey 与设备所属产品不匹配: demo-device-01", ex.getMessage());
        verify(mqttMessageConsumer).isConnected();
        verify(deviceService).getRequiredByCode("demo-device-01");
        verify(productService).getRequiredById(1001L);
        verifyNoInteractions(mqttDownMessagePublisher);
    }

    @Test
    void publishShouldPreserveFrameBytesWhenLatin1EncodingSpecified() {
        Device device = buildDevice();
        Product product = buildProduct();
        byte[] rawPacket = new byte[]{0x02, 0x01, (byte) 0xC8, 0x7B, 0x7D};

        when(mqttMessageConsumer.isConnected()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties()
        );

        MqttReportPublishCommand command = buildCommand(new String(rawPacket, StandardCharsets.ISO_8859_1));
        command.setPayloadEncoding("ISO-8859-1");

        service.publish(command);

        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mqttDownMessagePublisher).publishRaw(
                org.mockito.Mockito.eq("$dp"),
                payloadCaptor.capture(),
                org.mockito.Mockito.eq(1),
                org.mockito.Mockito.eq(false)
        );
        assertArrayEquals(rawPacket, payloadCaptor.getValue());
    }

    @Test
    void publishShouldUseExplicitQosAndRetainedWhenProvided() {
        Device device = buildDevice();
        Product product = buildProduct();

        when(mqttMessageConsumer.isConnected()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties()
        );

        MqttReportPublishCommand command = buildCommand("plain-text");
        command.setQos(2);
        command.setRetained(true);

        service.publish(command);

        verify(mqttDownMessagePublisher).publishRaw(
                "$dp",
                "plain-text".getBytes(StandardCharsets.UTF_8),
                2,
                true
        );
        verify(mqttMessageConsumer, never()).publish(
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.anyInt(),
                org.mockito.Mockito.anyBoolean()
        );
    }

    private Device buildDevice() {
        Device device = new Device();
        device.setId(2001L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        device.setProtocolCode("mqtt-json");
        return device;
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        return product;
    }

    private MqttReportPublishCommand buildCommand(String payload) {
        MqttReportPublishCommand command = new MqttReportPublishCommand();
        command.setProtocolCode("mqtt-json");
        command.setProductKey("demo-product");
        command.setDeviceCode("demo-device-01");
        command.setTopic("$dp");
        command.setPayload(payload);
        return command;
    }

    private IotProperties buildIotProperties() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Mqtt mqtt = new IotProperties.Mqtt();
        mqtt.setQos(1);
        iotProperties.setMqtt(mqtt);
        return iotProperties;
    }
}
