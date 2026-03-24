package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowMetricsRecorder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSession;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStatuses;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimelineStore;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    @Mock
    private MessageFlowMetricsRecorder messageFlowMetricsRecorder;
    @Mock
    private MessageFlowTimelineStore messageFlowTimelineStore;

    @Test
    void publishShouldRejectWhenBrokerUnavailable() {
        when(mqttMessageConsumer.isPublishCapable()).thenReturn(false);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties(),
                buildMessageFlowProperties(false),
                messageFlowMetricsRecorder,
                messageFlowTimelineStore
        );

        BizException ex = assertThrows(BizException.class, () -> service.publish(buildCommand("plain-text")));
        assertEquals("MQTT broker 未配置，无法执行模拟上报", ex.getMessage());
        verify(mqttMessageConsumer).isPublishCapable();
        verifyNoInteractions(deviceService, productService, mqttDownMessagePublisher);
    }

    @Test
    void publishShouldRejectWhenProductMismatched() {
        Device device = buildDevice();
        Product product = buildProduct();
        product.setProductKey("other-product");

        when(mqttMessageConsumer.isPublishCapable()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties(),
                buildMessageFlowProperties(false),
                messageFlowMetricsRecorder,
                messageFlowTimelineStore
        );

        BizException ex = assertThrows(BizException.class, () -> service.publish(buildCommand("plain-text")));
        assertEquals("模拟上报 productKey 与设备所属产品不匹配: demo-device-01", ex.getMessage());
        verify(mqttMessageConsumer).isPublishCapable();
        verify(deviceService).getRequiredByCode("demo-device-01");
        verify(productService).getRequiredById(1001L);
        verifyNoInteractions(mqttDownMessagePublisher);
    }

    @Test
    void publishShouldRejectWhenDeviceProductMissing() {
        Device device = buildDevice();
        device.setProductId(0L);

        when(mqttMessageConsumer.isPublishCapable()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties(),
                buildMessageFlowProperties(false),
                messageFlowMetricsRecorder,
                messageFlowTimelineStore
        );

        BizException ex = assertThrows(BizException.class, () -> service.publish(buildCommand("plain-text")));
        assertEquals("设备所属产品不存在: demo-device-01", ex.getMessage());
        verifyNoInteractions(productService, mqttDownMessagePublisher);
    }

    @Test
    void publishShouldFallbackToProductProtocolWhenDeviceProtocolBlank() {
        Device device = buildDevice();
        device.setProtocolCode("");
        Product product = buildProduct();

        when(mqttMessageConsumer.isPublishCapable()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties(),
                buildMessageFlowProperties(false),
                messageFlowMetricsRecorder,
                messageFlowTimelineStore
        );

        service.publish(buildCommand("plain-text"));

        verify(mqttDownMessagePublisher).publishRaw(
                "$dp",
                "plain-text".getBytes(StandardCharsets.UTF_8),
                1,
                false
        );
    }

    @Test
    void publishShouldRejectWhenProtocolMismatchedWithExpectedActual() {
        Device device = buildDevice();
        device.setProtocolCode("");
        Product product = buildProduct();
        product.setProtocolCode("tcp-hex");

        when(mqttMessageConsumer.isPublishCapable()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties(),
                buildMessageFlowProperties(false),
                messageFlowMetricsRecorder,
                messageFlowTimelineStore
        );

        BizException ex = assertThrows(BizException.class, () -> service.publish(buildCommand("plain-text")));
        assertEquals("模拟上报 protocolCode 与设备协议不匹配: demo-device-01, expected=tcp-hex, actual=mqtt-json", ex.getMessage());
        verifyNoInteractions(mqttDownMessagePublisher);
    }

    @Test
    void publishShouldPreserveFrameBytesWhenLatin1EncodingSpecified() {
        Device device = buildDevice();
        Product product = buildProduct();
        byte[] rawPacket = new byte[]{0x02, 0x01, (byte) 0xC8, 0x7B, 0x7D};

        when(mqttMessageConsumer.isPublishCapable()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties(),
                buildMessageFlowProperties(false),
                messageFlowMetricsRecorder,
                messageFlowTimelineStore
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

        when(mqttMessageConsumer.isPublishCapable()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties(),
                buildMessageFlowProperties(false),
                messageFlowMetricsRecorder,
                messageFlowTimelineStore
        );

        MqttReportPublishCommand command = buildCommand("plain-text");
        command.setQos(2);
        command.setRetained(true);

        MessageFlowSubmitResult submitResult = service.publish(command);

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
        assertEquals(MessageFlowStatuses.SESSION_PUBLISHED, submitResult.getStatus());
        assertFalse(Boolean.TRUE.equals(submitResult.getTimelineAvailable()));
        assertTrue(Boolean.TRUE.equals(submitResult.getCorrelationPending()));
    }

    @Test
    void publishShouldCreatePendingSessionAndFingerprintWhenMessageFlowEnabled() {
        Device device = buildDevice();
        Product product = buildProduct();

        when(mqttMessageConsumer.isPublishCapable()).thenReturn(true);
        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        MqttReportPublishServiceImpl service = new MqttReportPublishServiceImpl(
                deviceService,
                productService,
                mqttMessageConsumer,
                mqttDownMessagePublisher,
                buildIotProperties(),
                buildMessageFlowProperties(true),
                messageFlowMetricsRecorder,
                messageFlowTimelineStore
        );

        MessageFlowSubmitResult submitResult = service.publish(buildCommand("plain-text"));

        assertEquals(MessageFlowStatuses.SESSION_PUBLISHED, submitResult.getStatus());
        assertFalse(Boolean.TRUE.equals(submitResult.getTimelineAvailable()));
        assertTrue(Boolean.TRUE.equals(submitResult.getCorrelationPending()));
        assertTrue(submitResult.getSessionId() != null && !submitResult.getSessionId().isBlank());

        ArgumentCaptor<MessageFlowSession> sessionCaptor = ArgumentCaptor.forClass(MessageFlowSession.class);
        verify(messageFlowTimelineStore).saveSession(sessionCaptor.capture());
        MessageFlowSession session = sessionCaptor.getValue();
        assertEquals(submitResult.getSessionId(), session.getSessionId());
        assertEquals("MQTT", session.getTransportMode());
        assertEquals(MessageFlowStatuses.SESSION_PUBLISHED, session.getStatus());
        assertEquals("demo-device-01", session.getDeviceCode());
        assertEquals("$dp", session.getTopic());
        assertTrue(Boolean.TRUE.equals(session.getCorrelationPending()));

        verify(messageFlowTimelineStore).bindFingerprint(any(String.class), org.mockito.Mockito.eq(submitResult.getSessionId()));
        verify(mqttDownMessagePublisher, times(1)).publishRaw(
                "$dp",
                "plain-text".getBytes(StandardCharsets.UTF_8),
                1,
                false
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
        product.setProtocolCode("mqtt-json");
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

    private MessageFlowProperties buildMessageFlowProperties(boolean enabled) {
        MessageFlowProperties properties = new MessageFlowProperties();
        properties.setEnabled(enabled);
        properties.setTtlHours(24);
        properties.setSessionMatchWindowSeconds(120);
        return properties;
    }
}
