package com.ghlzm.iot.admin.observability.alerting;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import com.ghlzm.iot.system.service.NotificationChannelService;
import com.ghlzm.iot.system.service.NotificationHttpClient;
import com.ghlzm.iot.system.service.impl.NotificationChannelDispatcherImpl;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityAlertNotificationServiceTest {

    @Test
    void shouldRouteObservabilityAlertSceneAndBuildPayload() {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        IotProperties properties = new IotProperties();
        properties.getObservability().getAlerting().setScene("observability_alert");
        NotificationChannelDispatcher dispatcher = new NotificationChannelDispatcherImpl(
                notificationChannelService(List.of(
                        webhookChannel("ops-observability", "{\"url\":\"https://notify.example.com/ops\",\"scenes\":[\"observability_alert\"]}"),
                        webhookChannel("ops-system-error", "{\"url\":\"https://notify.example.com/system\",\"scenes\":[\"system_error\"]}")
                )),
                httpClient,
                properties,
                JsonMapper.builder().findAndAddModules().build()
        );
        ObservabilityAlertNotificationService service =
                new ObservabilityAlertNotificationService(dispatcher, properties);

        ObservabilityAlertNotificationService.DispatchSummary summary = service.dispatchAlert(
                new ObservabilityAlertTrigger(
                        "failure-stage-spike",
                        "protocol_decode",
                        "failureStage=protocol_decode",
                        "最近窗口内同一 failureStage 失败数",
                        12,
                        10,
                        10,
                        null,
                        "最近 10 分钟内 failureStage=protocol_decode 失败达到 12 次，触发阈值 10 次。",
                        Map.of("channelCode", "ops-observability")
                )
        );

        assertEquals("observability_alert", summary.scene());
        assertEquals(1, summary.channelCount());
        assertEquals(1, summary.successCount());
        assertEquals(List.of("ops-observability"), summary.channelCodes());
        assertEquals(1, httpClient.requests.size());
        assertEquals("https://notify.example.com/ops", httpClient.requests.get(0).url());
        assertTrue(httpClient.requests.get(0).body().contains("failure-stage-spike"));
        assertTrue(httpClient.requests.get(0).body().contains("protocol_decode"));
        assertTrue(httpClient.requests.get(0).body().contains("observability_alert"));
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
