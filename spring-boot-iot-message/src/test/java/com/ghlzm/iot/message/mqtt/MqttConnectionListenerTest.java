package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.framework.observability.BackendExceptionRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MqttConnectionListenerTest {

    @Test
    void shouldRecordMessageDispatchFailure() {
        AtomicReference<BackendExceptionEvent> captured = new AtomicReference<>();
        MqttConnectionListener listener = newListener(captured);
        BizException ex = new BizException("设备不存在: demo-device-02");

        listener.onMessageDispatchFailed("/sys/demo-product/demo-device-02/thing/property/post", ex);

        BackendExceptionEvent event = captured.get();
        assertNotNull(event);
        assertEquals("message.mqtt", event.operationModule());
        assertEquals("MqttMessageConsumer#messageArrived", event.operationMethod());
        assertEquals("/sys/demo-product/demo-device-02/thing/property/post", event.requestUrl());
        assertEquals("MQTT", event.requestMethod());
        assertEquals("messageDispatchFailed", event.context().get("event"));
        assertSame(ex, event.throwable());
    }

    @Test
    void shouldRecordSubscribeFailureTopics() {
        AtomicReference<BackendExceptionEvent> captured = new AtomicReference<>();
        MqttConnectionListener listener = newListener(captured);
        RuntimeException ex = new RuntimeException("subscribe failed");
        List<String> topics = List.of("/sys/+/+/thing/property/post");

        listener.onSubscribeFailed(topics, ex);

        BackendExceptionEvent event = captured.get();
        assertNotNull(event);
        assertEquals("MqttMessageConsumer#subscribeConfiguredTopics", event.operationMethod());
        assertEquals("subscribe", event.requestUrl());
        assertEquals(topics, event.context().get("topics"));
        assertSame(ex, event.throwable());
    }

    private MqttConnectionListener newListener(AtomicReference<BackendExceptionEvent> captured) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("backendExceptionRecorder", (BackendExceptionRecorder) captured::set);
        return new MqttConnectionListener(beanFactory.getBeanProvider(BackendExceptionRecorder.class));
    }
}
