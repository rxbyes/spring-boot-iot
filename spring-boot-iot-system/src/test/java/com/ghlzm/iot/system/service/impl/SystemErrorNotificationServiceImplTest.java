package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelService;
import com.ghlzm.iot.system.service.NotificationHttpClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemErrorNotificationServiceImplTest {

    @Test
    void shouldNotifyConfiguredSystemErrorChannels() {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        SystemErrorNotificationServiceImpl service = newService(
                enabledProperties(),
                List.of(
                        webhookChannel("ops-webhook", "{\"url\":\"https://notify.example.com/hook\",\"scenes\":[\"system_error\"],\"minIntervalSeconds\":300}"),
                        webhookChannel("ops-webhook-skip", "{\"url\":\"https://notify.example.com/skip\",\"scenes\":[\"alarm\"]}"),
                        emailChannel("email-default")
                ),
                httpClient
        );

        service.notifySystemError(
                new BackendExceptionEvent(
                        "message.mqtt",
                        "MqttMessageConsumer#messageArrived",
                        "/sys/demo-product/demo-device-02/thing/property/post",
                        "MQTT",
                        Map.of("event", "messageDispatchFailed"),
                        new BizException("设备不存在: demo-device-02")
                ),
                auditLog(1001L)
        );

        assertEquals(1, httpClient.requests.size());
        assertEquals("https://notify.example.com/hook", httpClient.requests.get(0).url());
        assertTrue(httpClient.requests.get(0).body().contains("system_error"));
        assertTrue(httpClient.requests.get(0).body().contains("1001"));
    }

    @Test
    void shouldThrottleDuplicateNotifications() {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        SystemErrorNotificationServiceImpl service = newService(
                enabledProperties(),
                List.of(webhookChannel("ops-webhook", "{\"url\":\"https://notify.example.com/hook\",\"scenes\":[\"system_error\"],\"minIntervalSeconds\":300}")),
                httpClient
        );

        BackendExceptionEvent event = new BackendExceptionEvent(
                "message.mqtt",
                "MqttMessageConsumer#messageArrived",
                "/sys/demo-product/demo-device-02/thing/property/post",
                "MQTT",
                Map.of(),
                new BizException("设备不存在: demo-device-02")
        );

        service.notifySystemError(event, auditLog(2001L));
        service.notifySystemError(event, auditLog(2002L));

        assertEquals(1, httpClient.requests.size());
    }

    @Test
    void shouldSendTestNotificationWithoutSceneRequirement() {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        SystemErrorNotificationServiceImpl service = newService(
                disabledProperties(),
                List.of(webhookChannel("ops-webhook", "{\"url\":\"https://notify.example.com/hook\"}")),
                httpClient
        );

        service.sendTestNotification("ops-webhook");

        assertEquals(1, httpClient.requests.size());
        assertTrue(httpClient.requests.get(0).body().contains("通知渠道测试"));
    }

    @Test
    void shouldRejectUnsupportedChannelTypeWhenTesting() {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        SystemErrorNotificationServiceImpl service = newService(
                enabledProperties(),
                List.of(emailChannel("email-default")),
                httpClient
        );

        assertThrows(BizException.class, () -> service.sendTestNotification("email-default"));
    }

    private SystemErrorNotificationServiceImpl newService(IotProperties properties,
                                                          List<NotificationChannel> channels,
                                                          RecordingHttpClient httpClient) {
        return new SystemErrorNotificationServiceImpl(
                notificationChannelService(channels),
                httpClient,
                properties,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    private NotificationChannelService notificationChannelService(List<NotificationChannel> channels) {
        return (NotificationChannelService) Proxy.newProxyInstance(
                NotificationChannelService.class.getClassLoader(),
                new Class[]{NotificationChannelService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "listChannels" -> channels;
                    case "getByCode" -> channels.stream()
                            .filter(channel -> channel.getChannelCode().equals(args[0]))
                            .findFirst()
                            .orElse(null);
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> type) {
        if (type.equals(boolean.class)) {
            return false;
        }
        if (type.equals(int.class) || type.equals(long.class) || type.equals(short.class) || type.equals(byte.class)) {
            return 0;
        }
        if (type.equals(float.class) || type.equals(double.class)) {
            return 0.0;
        }
        return null;
    }

    private NotificationChannel webhookChannel(String channelCode, String config) {
        NotificationChannel channel = new NotificationChannel();
        channel.setChannelCode(channelCode);
        channel.setChannelName(channelCode);
        channel.setChannelType("webhook");
        channel.setConfig(config);
        channel.setStatus(1);
        channel.setDeleted(0);
        return channel;
    }

    private NotificationChannel emailChannel(String channelCode) {
        NotificationChannel channel = new NotificationChannel();
        channel.setChannelCode(channelCode);
        channel.setChannelName(channelCode);
        channel.setChannelType("email");
        channel.setConfig("{\"host\":\"smtp.example.com\"}");
        channel.setStatus(1);
        channel.setDeleted(0);
        return channel;
    }

    private AuditLog auditLog(Long id) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(id);
        auditLog.setOperationModule("message.mqtt");
        auditLog.setOperationMethod("MqttMessageConsumer#messageArrived");
        auditLog.setRequestUrl("/sys/demo-product/demo-device-02/thing/property/post");
        auditLog.setRequestMethod("MQTT");
        auditLog.setResultMessage("BizException: 设备不存在: demo-device-02");
        auditLog.setOperationTime(new Date());
        return auditLog;
    }

    private IotProperties enabledProperties() {
        IotProperties properties = new IotProperties();
        properties.getObservability().setSystemErrorNotifyEnabled(true);
        properties.getObservability().setNotificationTimeoutMs(3000);
        properties.getObservability().setSystemErrorNotifyCooldownSeconds(300);
        return properties;
    }

    private IotProperties disabledProperties() {
        IotProperties properties = new IotProperties();
        properties.getObservability().setSystemErrorNotifyEnabled(false);
        properties.getObservability().setNotificationTimeoutMs(3000);
        properties.getObservability().setSystemErrorNotifyCooldownSeconds(300);
        return properties;
    }

    private static final class RecordingHttpClient implements NotificationHttpClient {
        private final List<Request> requests = new ArrayList<>();

        @Override
        public HttpResult postJson(String url, Map<String, String> headers, String body, Duration timeout) {
            requests.add(new Request(url, headers, body, timeout));
            return new HttpResult(200, "{\"ok\":true}");
        }

        private record Request(String url,
                               Map<String, String> headers,
                               String body,
                               Duration timeout) {
        }
    }
}
