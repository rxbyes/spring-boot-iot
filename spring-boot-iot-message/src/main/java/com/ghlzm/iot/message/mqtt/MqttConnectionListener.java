package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.framework.observability.BackendExceptionRecorder;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
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
    private final ObjectProvider<MqttConsumerRuntimeState> mqttConsumerRuntimeStateProvider;

    @Autowired
    public MqttConnectionListener(ObjectProvider<BackendExceptionRecorder> backendExceptionRecorderProvider,
                                  ObjectProvider<DeviceAccessErrorLogService> deviceAccessErrorLogServiceProvider,
                                  ObjectProvider<DeviceMessageService> deviceMessageServiceProvider,
                                  ObjectProvider<MqttConsumerRuntimeState> mqttConsumerRuntimeStateProvider) {
        this.backendExceptionRecorderProvider = backendExceptionRecorderProvider;
        this.deviceAccessErrorLogServiceProvider = deviceAccessErrorLogServiceProvider;
        this.deviceMessageServiceProvider = deviceMessageServiceProvider;
        this.mqttConsumerRuntimeStateProvider = mqttConsumerRuntimeStateProvider;
    }

    MqttConnectionListener(ObjectProvider<BackendExceptionRecorder> backendExceptionRecorderProvider,
                           ObjectProvider<DeviceAccessErrorLogService> deviceAccessErrorLogServiceProvider) {
        this(backendExceptionRecorderProvider, deviceAccessErrorLogServiceProvider, null, null);
    }

    public void onConnectComplete(boolean reconnect, String serverUri) {
        onConnectComplete(reconnect, serverUri, null);
    }

    public void onConnectComplete(boolean reconnect, String serverUri, String clientId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("clientId", clientId);
        details.put("reconnect", reconnect);
        details.put("serverUri", serverUri);
        log.info(ObservabilityEventLogSupport.summary("mqtt_connection", "success", null, details));
        runtimeState().ifPresent(MqttConsumerRuntimeState::markConnected);
    }

    public void onSubscribe(List<String> topics) {
        onSubscribe(topics, null);
    }

    public void onSubscribe(List<String> topics, String clientId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("clientId", clientId);
        details.put("topicCount", topics == null ? 0 : topics.size());
        details.put("topics", topics);
        log.info(ObservabilityEventLogSupport.summary("mqtt_subscribe", "success", null, details));
        runtimeState().ifPresent(state -> state.markSubscribed(topics));
    }

    public void onMessageReceived(String topic, int payloadSize) {
        if (!log.isTraceEnabled()) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("topic", topic);
        details.put("payloadSize", payloadSize);
        log.trace(ObservabilityEventLogSupport.summary("mqtt_receive", "success", null, details));
    }

    public void onMessageDispatched(String topic, String deviceCode, String messageType, String traceId) {
        if (!log.isTraceEnabled()) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", traceId);
        details.put("topic", topic);
        details.put("deviceCode", deviceCode);
        details.put("messageType", messageType);
        log.trace(ObservabilityEventLogSupport.summary("mqtt_dispatch", "success", null, details));
    }

    public void onConnectionLost(Throwable cause) {
        onConnectionLost(cause, null);
    }

    public void onConnectionLost(Throwable cause, String clientId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("clientId", clientId);
        details.put("reason", cause == null ? "unknown" : cause.getMessage());
        if (cause != null) {
            details.put("errorClass", cause.getClass().getSimpleName());
        }
        log.warn(ObservabilityEventLogSupport.summary("mqtt_connection", "failure", null, details));
        runtimeState().ifPresent(state -> state.markDisconnected("connection", TraceContextHolder.getTraceId()));
        if (cause != null) {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("event", "connectionLost");
            putIfHasText(context, "clientId", clientId);
            recordBackendException(
                    "MqttMessageConsumer#connectionLost",
                    "connection",
                    context,
                    cause
            );
        }
    }

    public void onStartupSkipped(String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("reason", reason);
        log.info(ObservabilityEventLogSupport.summary("mqtt_startup", "skipped", null, details));
    }

    public void onStartupFailed(Throwable throwable) {
        onStartupFailed(throwable, null);
    }

    public void onStartupFailed(Throwable throwable, String clientId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("clientId", clientId);
        details.put("errorClass", throwable == null ? null : throwable.getClass().getSimpleName());
        log.error(ObservabilityEventLogSupport.summary("mqtt_startup", "failure", null, details), throwable);
        runtimeState().ifPresent(state -> state.markDisconnected("startup", TraceContextHolder.getTraceId()));
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("event", "startupFailed");
        putIfHasText(context, "clientId", clientId);
        recordBackendException(
                "MqttMessageConsumer#start",
                "startup",
                context,
                throwable
        );
    }

    public void onShutdownFailed(Throwable throwable) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("errorClass", throwable == null ? null : throwable.getClass().getSimpleName());
        log.error(ObservabilityEventLogSupport.summary("mqtt_shutdown", "failure", null, details), throwable);
        recordBackendException(
                "MqttMessageConsumer#stop",
                "shutdown",
                Map.of("event", "shutdownFailed"),
                throwable
        );
    }

    public void onSubscribeFailed(List<String> topics, Throwable throwable) {
        onSubscribeFailed(topics, throwable, null);
    }

    public void onSubscribeFailed(List<String> topics, Throwable throwable, String clientId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("clientId", clientId);
        details.put("topics", topics);
        details.put("errorClass", throwable == null ? null : throwable.getClass().getSimpleName());
        log.error(ObservabilityEventLogSupport.summary("mqtt_subscribe", "failure", null, details), throwable);
        runtimeState().ifPresent(state -> state.markFailure("subscribe", TraceContextHolder.getTraceId()));
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("event", "subscribeFailed");
        context.put("topics", topics);
        putIfHasText(context, "clientId", clientId);
        recordBackendException(
                "MqttMessageConsumer#subscribeConfiguredTopics",
                "subscribe",
                context,
                throwable
        );
    }

    public void onMessageDispatchFailed(String topic, byte[] payload, RawDeviceMessage rawDeviceMessage, Throwable throwable) {
        String failureStage = resolveFailureStage(rawDeviceMessage, throwable);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", rawDeviceMessage == null ? TraceContextHolder.getTraceId() : rawDeviceMessage.getTraceId());
        details.put("topic", topic);
        details.put("failureStage", failureStage);
        if (rawDeviceMessage != null) {
            details.put("deviceCode", rawDeviceMessage.getDeviceCode());
            details.put("productKey", rawDeviceMessage.getProductKey());
            details.put("protocolCode", rawDeviceMessage.getProtocolCode());
            details.put("messageType", rawDeviceMessage.getMessageType());
            details.put("clientId", rawDeviceMessage.getClientId());
        }
        details.put("errorClass", throwable == null ? null : throwable.getClass().getSimpleName());
        log.error(ObservabilityEventLogSupport.summary("mqtt_dispatch", "failure", null, details), throwable);
        runtimeState().ifPresent(state -> state.markFailure(
                failureStage,
                rawDeviceMessage == null ? null : rawDeviceMessage.getTraceId()
        ));
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("event", "messageDispatchFailed");
        context.put("topic", topic);
        context.put("failureStage", failureStage);
        if (rawDeviceMessage != null) {
            putIfHasText(context, "traceId", rawDeviceMessage.getTraceId());
            putIfHasText(context, "deviceCode", rawDeviceMessage.getDeviceCode());
            putIfHasText(context, "productKey", rawDeviceMessage.getProductKey());
            putIfHasText(context, "protocolCode", rawDeviceMessage.getProtocolCode());
            putIfHasText(context, "messageType", rawDeviceMessage.getMessageType());
            putIfHasText(context, "topicRouteType", rawDeviceMessage.getTopicRouteType());
            putIfHasText(context, "clientId", rawDeviceMessage.getClientId());
            putIfHasText(context, "gatewayDeviceCode", rawDeviceMessage.getGatewayDeviceCode());
            putIfHasText(context, "subDeviceCode", rawDeviceMessage.getSubDeviceCode());
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
        if (!log.isTraceEnabled()) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("deviceCode", deviceCode);
        details.put("clientId", clientId);
        details.put("topic", topic);
        log.trace(ObservabilityEventLogSupport.summary("mqtt_session_refresh", "success", null, details));
    }

    private java.util.Optional<MqttConsumerRuntimeState> runtimeState() {
        if (mqttConsumerRuntimeStateProvider == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(mqttConsumerRuntimeStateProvider.getIfAvailable());
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
        if (errorText.contains("设备不存在")
                || errorText.contains("协议不匹配")
                || errorText.contains("协议未配置")
                || errorText.contains("协议配置异常")
                || errorText.contains("产品不匹配")
                || errorText.contains("产品不存在")
                || errorText.contains("未绑定产品")) {
            return "device_validate";
        }
        return "message_dispatch";
    }
}
