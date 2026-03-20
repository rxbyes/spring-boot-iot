package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.framework.observability.BackendExceptionRecorder;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ObjectProvider<DeviceAccessErrorLogService> deviceAccessErrorLogServiceProvider;
    private final ObjectProvider<DeviceMessageService> deviceMessageServiceProvider;

    @Autowired
    public MqttConnectionListener(ObjectProvider<BackendExceptionRecorder> backendExceptionRecorderProvider,
                                  ObjectProvider<DeviceAccessErrorLogService> deviceAccessErrorLogServiceProvider,
                                  ObjectProvider<DeviceMessageService> deviceMessageServiceProvider) {
        this.backendExceptionRecorderProvider = backendExceptionRecorderProvider;
        this.deviceAccessErrorLogServiceProvider = deviceAccessErrorLogServiceProvider;
        this.deviceMessageServiceProvider = deviceMessageServiceProvider;
    }

    MqttConnectionListener(ObjectProvider<BackendExceptionRecorder> backendExceptionRecorderProvider,
                           ObjectProvider<DeviceAccessErrorLogService> deviceAccessErrorLogServiceProvider) {
        this(backendExceptionRecorderProvider, deviceAccessErrorLogServiceProvider, null);
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

    public void onMessageDispatchFailed(String topic, byte[] payload, RawDeviceMessage rawDeviceMessage, Throwable throwable) {
        log.error("MQTT 消息分发失败, topic={}", topic, throwable);
        String failureStage = resolveFailureStage(rawDeviceMessage, throwable);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("event", "messageDispatchFailed");
        context.put("topic", topic);
        context.put("failureStage", failureStage);
        if (rawDeviceMessage != null) {
            putIfHasText(context, "traceId", rawDeviceMessage.getTraceId());
            putIfHasText(context, "deviceCode", rawDeviceMessage.getDeviceCode());
            putIfHasText(context, "productKey", rawDeviceMessage.getProductKey());
            putIfHasText(context, "messageType", rawDeviceMessage.getMessageType());
            putIfHasText(context, "topicRouteType", rawDeviceMessage.getTopicRouteType());
        }
        archiveFailureTrace(topic, payload, rawDeviceMessage);
        archiveAccessFailure(topic, payload, rawDeviceMessage, failureStage, throwable);
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

    private void archiveAccessFailure(String topic,
                                      byte[] payload,
                                      RawDeviceMessage rawDeviceMessage,
                                      String failureStage,
                                      Throwable throwable) {
        DeviceAccessErrorLogService service = deviceAccessErrorLogServiceProvider.getIfAvailable();
        if (service == null || throwable == null) {
            return;
        }
        try {
            service.archiveMqttFailure(topic, payload, rawDeviceMessage, failureStage, throwable);
        } catch (Exception ex) {
            log.warn("归档失败报文失败, topic={}, error={}", topic, ex.getMessage());
        }
    }

    private void archiveFailureTrace(String topic, byte[] payload, RawDeviceMessage rawDeviceMessage) {
        if (deviceMessageServiceProvider == null) {
            return;
        }
        DeviceMessageService service = deviceMessageServiceProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            service.recordDispatchFailureTrace(topic, payload, rawDeviceMessage);
        } catch (Exception ex) {
            log.warn("写入失败轨迹日志失败, topic={}, error={}", topic, ex.getMessage());
        }
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

    private String resolveFailureStage(RawDeviceMessage rawDeviceMessage, Throwable throwable) {
        // 简单按路由/解码/设备校验/后续分发四段区分失败阶段，方便测试与研发联动排障。
        if (rawDeviceMessage == null) {
            return "topic_route";
        }
        String errorText = throwable == null ? "" : ((throwable.getClass().getName() + " " + throwable.getMessage()).toLowerCase());
        if (errorText.contains("协议解析") || errorText.contains("decode")) {
            return "protocol_decode";
        }
        if (errorText.contains("设备不存在") || errorText.contains("协议不匹配") || errorText.contains("产品不匹配")) {
            return "device_validate";
        }
        return "message_dispatch";
    }
}
