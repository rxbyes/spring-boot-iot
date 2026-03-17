package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.framework.observability.BackendExceptionRecorder;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MQTT 连接监听器。
 */
@Component
public class MqttConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(MqttConnectionListener.class);
    private static final String MQTT_MODULE = "message.mqtt";
    private static final String MQTT_METHOD = "MQTT";

    private final ObjectProvider<BackendExceptionRecorder> backendExceptionRecorderProvider;

    public MqttConnectionListener(ObjectProvider<BackendExceptionRecorder> backendExceptionRecorderProvider) {
        this.backendExceptionRecorderProvider = backendExceptionRecorderProvider;
    }

    public void onConnectComplete(boolean reconnect, String serverUri) {
        log.info("MQTT 客户端已连接, reconnect={}, serverUri={}", reconnect, serverUri);
    }

    public void onSubscribe(List<String> topics) {
        log.info("MQTT 客户端已订阅主题: {}", topics);
    }

    public void onMessageReceived(String topic, int payloadSize) {
        log.info("MQTT 收到上行消息, topic={}, payloadSize={}", topic, payloadSize);
    }

    public void onMessageDispatched(String topic, String deviceCode, String messageType, String traceId) {
        log.info("MQTT 上行消息进入主链路成功, topic={}, deviceCode={}, messageType={}, traceId={}",
                topic, deviceCode, messageType, traceId);
    }

    public void onConnectionLost(Throwable cause) {
        log.warn("MQTT 连接已断开: {}", cause == null ? "unknown" : cause.getMessage());
        if (cause != null) {
            recordBackendException(
                    "MqttMessageConsumer#connectionLost",
                    "connection",
                    Map.of("event", "connectionLost"),
                    cause
            );
        }
    }

    public void onStartupSkipped(String reason) {
        log.info("跳过 MQTT 客户端启动: {}", reason);
    }

    public void onStartupFailed(Throwable throwable) {
        log.error("MQTT 客户端启动失败", throwable);
        recordBackendException(
                "MqttMessageConsumer#start",
                "startup",
                Map.of("event", "startupFailed"),
                throwable
        );
    }

    public void onShutdownFailed(Throwable throwable) {
        log.error("MQTT 客户端关闭失败", throwable);
        recordBackendException(
                "MqttMessageConsumer#stop",
                "shutdown",
                Map.of("event", "shutdownFailed"),
                throwable
        );
    }

    public void onSubscribeFailed(List<String> topics, Throwable throwable) {
        log.error("MQTT 客户端订阅主题失败, topics={}", topics, throwable);
        recordBackendException(
                "MqttMessageConsumer#subscribeConfiguredTopics",
                "subscribe",
                Map.of("event", "subscribeFailed", "topics", topics),
                throwable
        );
    }

    public void onMessageDispatchFailed(String topic, RawDeviceMessage rawDeviceMessage, Throwable throwable) {
        log.error("MQTT 消息分发失败, topic={}", topic, throwable);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("event", "messageDispatchFailed");
        context.put("topic", topic);
        if (rawDeviceMessage != null) {
            putIfHasText(context, "traceId", rawDeviceMessage.getTraceId());
            putIfHasText(context, "deviceCode", rawDeviceMessage.getDeviceCode());
            putIfHasText(context, "productKey", rawDeviceMessage.getProductKey());
            putIfHasText(context, "messageType", rawDeviceMessage.getMessageType());
            putIfHasText(context, "topicRouteType", rawDeviceMessage.getTopicRouteType());
        }
        recordBackendException(
                "MqttMessageConsumer#messageArrived",
                topic,
                context,
                throwable
        );
    }

    public void onDeviceSessionRefreshed(String deviceCode, String clientId, String topic) {
        log.debug("MQTT 设备会话已刷新, deviceCode={}, clientId={}, topic={}", deviceCode, clientId, topic);
    }

    private void recordBackendException(String operationMethod,
                                        String requestUrl,
                                        Map<String, Object> context,
                                        Throwable throwable) {
        BackendExceptionRecorder recorder = backendExceptionRecorderProvider.getIfAvailable();
        if (recorder == null || throwable == null) {
            return;
        }
        Map<String, Object> finalContext = new LinkedHashMap<>();
        if (context != null) {
            finalContext.putAll(context);
        }
        if (!finalContext.containsKey("traceId")) {
            putIfHasText(finalContext, "traceId", TraceContextHolder.getTraceId());
        }
        try {
            recorder.record(new BackendExceptionEvent(
                    MQTT_MODULE,
                    operationMethod,
                    requestUrl,
                    MQTT_METHOD,
                    finalContext,
                    throwable
            ));
        } catch (Exception ex) {
            log.warn("写入后台异常审计失败, requestUrl={}, error={}", requestUrl, ex.getMessage());
        }
    }

    private void putIfHasText(Map<String, Object> context, String key, String value) {
        if (context != null && StringUtils.hasText(value)) {
            context.put(key, value.trim());
        }
    }
}
