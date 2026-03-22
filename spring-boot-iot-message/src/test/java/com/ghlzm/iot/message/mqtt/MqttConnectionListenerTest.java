package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.framework.observability.BackendExceptionRecorder;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MqttConnectionListenerTest {

    @Test
    void shouldRecordMessageDispatchFailure() {
        AtomicReference<BackendExceptionEvent> captured = new AtomicReference<>();
        AtomicReference<ArchiveCall> archived = new AtomicReference<>();
        MqttConnectionListener listener = newListener(captured, archived);
        BizException ex = new BizException("设备不存在: demo-device-02");
        RawDeviceMessage rawDeviceMessage = new RawDeviceMessage();
        rawDeviceMessage.setTraceId("trace-demo-001");
        rawDeviceMessage.setDeviceCode("demo-device-02");
        rawDeviceMessage.setProductKey("demo-product");
        rawDeviceMessage.setMessageType("property");
        rawDeviceMessage.setTopicRouteType("direct");

        listener.onMessageDispatchFailed(
                "/sys/demo-product/demo-device-02/thing/property/post",
                "{\"temp\":26.5}".getBytes(StandardCharsets.UTF_8),
                rawDeviceMessage,
                ex
        );

        BackendExceptionEvent event = captured.get();
        assertNotNull(event);
        assertEquals("message.mqtt", event.operationModule());
        assertEquals("MqttMessageConsumer#messageArrived", event.operationMethod());
        assertEquals("/sys/demo-product/demo-device-02/thing/property/post", event.requestUrl());
        assertEquals("MQTT", event.requestMethod());
        assertEquals("messageDispatchFailed", event.context().get("event"));
        assertEquals("trace-demo-001", event.context().get("traceId"));
        assertEquals("demo-device-02", event.context().get("deviceCode"));
        assertEquals("demo-product", event.context().get("productKey"));
        assertEquals("property", event.context().get("messageType"));
        assertEquals("direct", event.context().get("topicRouteType"));
        assertEquals("device_validate", event.context().get("failureStage"));
        assertSame(ex, event.throwable());

        ArchiveCall archiveCall = archived.get();
        assertNotNull(archiveCall);
        assertEquals("/sys/demo-product/demo-device-02/thing/property/post", archiveCall.topic());
        assertEquals("device_validate", archiveCall.failureStage());
        assertSame(rawDeviceMessage, archiveCall.rawDeviceMessage());
        assertSame(ex, archiveCall.throwable());
    }

    @Test
    void shouldRecordSubscribeFailureTopics() {
        AtomicReference<BackendExceptionEvent> captured = new AtomicReference<>();
        MqttConnectionListener listener = newListener(captured, new AtomicReference<>());
        RuntimeException ex = new RuntimeException("subscribe failed");
        List<String> topics = List.of("/sys/+/+/thing/property/post");

        listener.onSubscribeFailed(topics, ex, "shared-client-dev-01");

        BackendExceptionEvent event = captured.get();
        assertNotNull(event);
        assertEquals("MqttMessageConsumer#subscribeConfiguredTopics", event.operationMethod());
        assertEquals("subscribe", event.requestUrl());
        assertEquals(topics, event.context().get("topics"));
        assertEquals("shared-client-dev-01", event.context().get("clientId"));
        assertSame(ex, event.throwable());
    }

    @Test
    void shouldRecordConnectionLostClientId() {
        AtomicReference<BackendExceptionEvent> captured = new AtomicReference<>();
        MqttConnectionListener listener = newListener(captured, new AtomicReference<>());
        RuntimeException ex = new RuntimeException("connection reset");

        listener.onConnectionLost(ex, "shared-client-dev-02");

        BackendExceptionEvent event = captured.get();
        assertNotNull(event);
        assertEquals("MqttMessageConsumer#connectionLost", event.operationMethod());
        assertEquals("shared-client-dev-02", event.context().get("clientId"));
        assertSame(ex, event.throwable());
    }

    private MqttConnectionListener newListener(AtomicReference<BackendExceptionEvent> captured,
                                               AtomicReference<ArchiveCall> archived) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("backendExceptionRecorder", (BackendExceptionRecorder) captured::set);
        beanFactory.addBean("deviceAccessErrorLogService", new DeviceAccessErrorLogService() {
            @Override
            public void archiveMqttFailure(String topic,
                                           byte[] payload,
                                           RawDeviceMessage rawDeviceMessage,
                                           String failureStage,
                                           Throwable throwable) {
                archived.set(new ArchiveCall(topic, payload, rawDeviceMessage, failureStage, throwable));
            }

            @Override
            public com.ghlzm.iot.common.response.PageResult<com.ghlzm.iot.device.entity.DeviceAccessErrorLog> pageLogs(
                    com.ghlzm.iot.device.dto.DeviceAccessErrorQuery query, Integer pageNum, Integer pageSize) {
                throw new UnsupportedOperationException();
            }

            @Override
            public com.ghlzm.iot.device.entity.DeviceAccessErrorLog getById(Long id) {
                throw new UnsupportedOperationException();
            }
        });
        return new MqttConnectionListener(
                beanFactory.getBeanProvider(BackendExceptionRecorder.class),
                beanFactory.getBeanProvider(DeviceAccessErrorLogService.class)
        );
    }

    private record ArchiveCall(String topic,
                               byte[] payload,
                               RawDeviceMessage rawDeviceMessage,
                               String failureStage,
                               Throwable throwable) {
    }
}
