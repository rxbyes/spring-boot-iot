package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.DiagnosticLoggingConstants;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.message.pipeline.MessageFlowExecutionResult;
import com.ghlzm.iot.message.pipeline.UpMessageProcessingPipeline;
import com.ghlzm.iot.message.pipeline.UpMessageProcessingRequest;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 消息接入消费者。
 */
@Slf4j
@Component
public class MqttMessageConsumer implements SmartLifecycle, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageConsumer.class);
    private static final Logger diagnosticAccessLog =
            LoggerFactory.getLogger(DiagnosticLoggingConstants.DIAGNOSTIC_ACCESS_LOGGER_NAME);

    private final IotProperties iotProperties;
    private final UpMessageProcessingPipeline upMessageProcessingPipeline;
    private final MqttTopicRouter mqttTopicRouter;
    private final MqttConnectionListener mqttConnectionListener;
    private final MqttConsumerRuntimeState mqttConsumerRuntimeState;
    private final MqttClusterLeadershipService mqttClusterLeadershipService;
    private final MqttInvalidReportGovernanceService mqttInvalidReportGovernanceService;

    private volatile boolean lifecycleRunning;
    private volatile boolean consumerActive;
    private volatile boolean leader;
    private MqttClient mqttClient;
    private String effectiveClientId;
    private String leadershipOwnerId;
    private volatile ScheduledExecutorService leadershipScheduler;
    private volatile long lastLeadershipAcquireAt;
    private volatile long lastLeadershipRenewAt;

    public MqttMessageConsumer(IotProperties iotProperties,
                               UpMessageProcessingPipeline upMessageProcessingPipeline,
                               MqttTopicRouter mqttTopicRouter,
                               MqttConnectionListener mqttConnectionListener,
                               MqttConsumerRuntimeState mqttConsumerRuntimeState,
                               MqttClusterLeadershipService mqttClusterLeadershipService,
                               MqttInvalidReportGovernanceService mqttInvalidReportGovernanceService) {
        this.iotProperties = iotProperties;
        this.upMessageProcessingPipeline = upMessageProcessingPipeline;
        this.mqttTopicRouter = mqttTopicRouter;
        this.mqttConnectionListener = mqttConnectionListener;
        this.mqttConsumerRuntimeState = mqttConsumerRuntimeState;
        this.mqttClusterLeadershipService = mqttClusterLeadershipService;
        this.mqttInvalidReportGovernanceService = mqttInvalidReportGovernanceService;
    }

    @Override
    public void start() {
        if (lifecycleRunning) {
            return;
        }
        if (!Boolean.TRUE.equals(iotProperties.getMqtt().getEnabled())) {
            mqttConnectionListener.onStartupSkipped("iot.mqtt.enabled=false");
            return;
        }
        if (iotProperties.getMqtt().getBrokerUrl() == null || iotProperties.getMqtt().getBrokerUrl().isBlank()) {
            mqttConnectionListener.onStartupSkipped("iot.mqtt.broker-url 未配置");
            return;
        }
        effectiveClientId = resolveClientId();
        leadershipOwnerId = effectiveClientId + ":" + UUID.randomUUID();
        lifecycleRunning = true;
        if (mqttClusterLeadershipService.isEnabled()) {
            startLeadershipScheduler();
            return;
        }
        startLocalConsumer();
    }

    @Override
    public void stop() {
        lifecycleRunning = false;
        stopLeadershipScheduler();
        synchronized (this) {
            stopLocalConsumer();
        }
        if (leader && mqttClusterLeadershipService.isEnabled() && hasText(leadershipOwnerId)) {
            try {
                mqttClusterLeadershipService.releaseLeadership(leadershipOwnerId);
            } catch (Exception ex) {
                LinkedHashMap<String, Object> details = new LinkedHashMap<>();
                details.put("ownerId", leadershipOwnerId);
                details.put("reason", ex.getMessage());
                logLeadershipEvent("release_failure", details);
            } finally {
                leader = false;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return lifecycleRunning;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        consumerActive = true;
        mqttConnectionListener.onConnectComplete(reconnect, serverURI, effectiveClientId);
        if (reconnect) {
            subscribeConfiguredTopics();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        consumerActive = false;
        mqttConnectionListener.onConnectionLost(cause, effectiveClientId);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        byte[] payload = message == null ? null : message.getPayload();
        log.info("MQTT Topic： {}，上报数据：{}", topic, resolvePayloadForLog(payload));

        long startNs = System.nanoTime();
        RawDeviceMessage rawDeviceMessage = null;
        DeviceUpMessage upMessage = null;
        Exception dispatchException = null;
        String traceId = TraceContextHolder.bindTraceId(null);
        try {
            mqttConnectionListener.onMessageReceived(topic, message == null || message.getPayload() == null
                    ? 0
                    : message.getPayload().length);
            mqttConsumerRuntimeState.markMessageReceived();
            if (payload == null || payload.length == 0) {
                InvalidMqttReportDecision decision = mqttInvalidReportGovernanceService.handleRawEmptyPayload(topic, null);
                if (decision.suppressed()) {
                    return;
                }
                rawDeviceMessage = buildGovernedRawMessage(topic, message, traceId);
                mqttConnectionListener.onGovernedMessageDispatchFailed(
                        topic,
                        payload,
                        rawDeviceMessage,
                        "protocol_decode",
                        new BizException("MQTT 负载不能为空")
                );
                return;
            }
            UpMessageProcessingRequest pipelineRequest = new UpMessageProcessingRequest();
            pipelineRequest.setTransportMode("MQTT");
            pipelineRequest.setTopic(topic);
            pipelineRequest.setPayload(payload);
            MessageFlowExecutionResult executionResult = upMessageProcessingPipeline.process(pipelineRequest);
            rawDeviceMessage = executionResult.getRawDeviceMessage();
            upMessage = executionResult.getUpMessage();
            mqttConsumerRuntimeState.markDispatchSuccess(upMessage.getTraceId());
            String resolvedDeviceCode = hasText(upMessage.getDeviceCode())
                    ? upMessage.getDeviceCode()
                    : rawDeviceMessage.getDeviceCode();
            mqttConnectionListener.onMessageDispatched(topic, resolvedDeviceCode, upMessage.getMessageType(), upMessage.getTraceId());

            String resolvedClientId = hasText(rawDeviceMessage.getClientId())
                    ? rawDeviceMessage.getClientId()
                    : resolvedDeviceCode;
            mqttConnectionListener.onDeviceSessionRefreshed(resolvedDeviceCode, resolvedClientId, topic);
        } catch (Exception ex) {
            dispatchException = ex;
            mqttConnectionListener.onMessageDispatchFailed(
                    topic,
                    message == null ? null : message.getPayload(),
                    rawDeviceMessage,
                    ex
            );
        } finally {
            long costMs = (System.nanoTime() - startNs) / 1_000_000L;
            maybeLogSlowDispatch(topic, rawDeviceMessage, upMessage, traceId, dispatchException, costMs);
            TraceContextHolder.clear();
        }
    }

    private RawDeviceMessage buildGovernedRawMessage(String topic, MqttMessage message, String traceId) {
        try {
            RawDeviceMessage rawDeviceMessage = mqttTopicRouter.toRawMessage(topic, message);
            rawDeviceMessage.setTraceId(traceId);
            return rawDeviceMessage;
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolvePayloadForLog(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "";
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 当前只处理上行接入。
    }

    /**
     * 复用同一 MQTT 客户端执行下行发布。
     */
    public void publish(String topic, byte[] payload, int qos, boolean retained) {
        if (mqttClient != null && mqttClient.isConnected()) {
            publishViaClient(mqttClient, topic, payload, qos, retained);
            return;
        }
        publishViaEphemeralClient(topic, payload, qos, retained);
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public boolean isConsumerActive() {
        return consumerActive;
    }

    public boolean isLeader() {
        return !mqttClusterLeadershipService.isEnabled() || leader;
    }

    public boolean isClusterSingletonEnabled() {
        return mqttClusterLeadershipService.isEnabled();
    }

    public String getLeadershipMode() {
        if (!mqttClusterLeadershipService.isEnabled()) {
            return "SINGLE";
        }
        return leader ? "LEADER" : "STANDBY";
    }

    public Optional<String> getCurrentLeaderOwnerId() {
        if (leader && hasText(leadershipOwnerId)) {
            return Optional.of(leadershipOwnerId);
        }
        try {
            return mqttClusterLeadershipService.getCurrentLeaderOwnerId();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public boolean isPublishCapable() {
        return iotProperties.getMqtt() != null
                && Boolean.TRUE.equals(iotProperties.getMqtt().getEnabled())
                && hasText(iotProperties.getMqtt().getBrokerUrl());
    }

    public String getEffectiveClientId() {
        return effectiveClientId;
    }

    private void startLeadershipScheduler() {
        if (leadershipScheduler != null && !leadershipScheduler.isShutdown()) {
            return;
        }
        leadershipScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "mqtt-leadership-maintainer");
            thread.setDaemon(true);
            return thread;
        });
        leadershipScheduler.scheduleWithFixedDelay(this::maintainLeadershipSafely, 0L, 1L, TimeUnit.SECONDS);
    }

    private void stopLeadershipScheduler() {
        ScheduledExecutorService scheduler = leadershipScheduler;
        leadershipScheduler = null;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void maintainLeadershipSafely() {
        try {
            maintainLeadership();
        } catch (Exception ex) {
            LinkedHashMap<String, Object> details = new LinkedHashMap<>();
            details.put("ownerId", leadershipOwnerId);
            details.put("reason", ex.getMessage());
            logLeadershipEvent("maintain_failure", details);
        }
    }

    private void maintainLeadership() {
        if (!lifecycleRunning || !mqttClusterLeadershipService.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (leader) {
            if (now - lastLeadershipRenewAt >= mqttClusterLeadershipService.resolveRenewInterval().toMillis()) {
                boolean renewed = mqttClusterLeadershipService.renewLeadership(leadershipOwnerId);
                lastLeadershipRenewAt = now;
                if (!renewed) {
                    leader = false;
                    logLeadershipEvent("lost", Map.of("ownerId", leadershipOwnerId));
                    synchronized (this) {
                        stopLocalConsumer();
                    }
                    return;
                }
            }
            synchronized (this) {
                if (!consumerActive) {
                    startLocalConsumer();
                }
            }
            return;
        }
        if (now - lastLeadershipAcquireAt < mqttClusterLeadershipService.resolveAcquireInterval().toMillis()) {
            return;
        }
        lastLeadershipAcquireAt = now;
        boolean acquired = mqttClusterLeadershipService.tryAcquireLeadership(leadershipOwnerId);
        if (!acquired) {
            synchronized (this) {
                stopLocalConsumer();
            }
            return;
        }
        leader = true;
        lastLeadershipRenewAt = now;
        logLeadershipEvent("acquired", Map.of("ownerId", leadershipOwnerId));
        synchronized (this) {
            startLocalConsumer();
        }
    }

    private synchronized void startLocalConsumer() {
        if (!lifecycleRunning) {
            return;
        }
        if (mqttClient != null && mqttClient.isConnected()) {
            consumerActive = true;
            return;
        }
        try {
            closeClientQuietly(mqttClient);
            mqttClient = new MqttClient(
                    iotProperties.getMqtt().getBrokerUrl(),
                    effectiveClientId,
                    new MemoryPersistence()
            );
            mqttClient.setCallback(this);
            mqttClient.connect(buildConnectOptions());
            subscribeConfiguredTopics();
            consumerActive = true;
        } catch (MqttException ex) {
            consumerActive = false;
            mqttConnectionListener.onStartupFailed(ex, effectiveClientId);
        }
    }

    private synchronized void stopLocalConsumer() {
        closeClientQuietly(mqttClient);
        mqttClient = null;
        consumerActive = false;
    }

    private void closeClientQuietly(MqttClient client) {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException ex) {
            mqttConnectionListener.onShutdownFailed(ex);
        }
    }

    private void publishViaClient(MqttClient client, String topic, byte[] payload, int qos, boolean retained) {
        try {
            MqttMessage mqttMessage = new MqttMessage(payload);
            mqttMessage.setQos(qos);
            mqttMessage.setRetained(retained);
            client.publish(topic, mqttMessage);
        } catch (MqttException ex) {
            throw new BizException("MQTT 消息发布失败");
        }
    }

    private void publishViaEphemeralClient(String topic, byte[] payload, int qos, boolean retained) {
        if (!isPublishCapable()) {
            throw new BizException("MQTT broker 未配置，无法发布消息");
        }
        String publisherClientId = effectiveClientId + "-publisher-" + UUID.randomUUID().toString().replace("-", "");
        MqttClient publisherClient = null;
        try {
            publisherClient = new MqttClient(
                    iotProperties.getMqtt().getBrokerUrl(),
                    publisherClientId,
                    new MemoryPersistence()
            );
            publisherClient.connect(buildConnectOptions());
            publishViaClient(publisherClient, topic, payload, qos, retained);
            publisherClient.disconnect();
        } catch (MqttException ex) {
            throw new BizException("MQTT 消息发布失败");
        } finally {
            closeClientQuietly(publisherClient);
        }
    }

    private void subscribeConfiguredTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        List<String> topics = mqttTopicRouter.resolveSubscribeTopics();
        try {
            for (String topic : topics) {
                mqttClient.subscribe(topic, iotProperties.getMqtt().getQos());
            }
            mqttConnectionListener.onSubscribe(topics, effectiveClientId);
        } catch (MqttException ex) {
            mqttConnectionListener.onSubscribeFailed(topics, ex, effectiveClientId);
        }
    }

    private void logLeadershipEvent(String result, Map<String, Object> details) {
        if (!log.isInfoEnabled()) {
            return;
        }
        LinkedHashMap<String, Object> finalDetails = new LinkedHashMap<>();
        finalDetails.put("traceId", TraceContextHolder.getTraceId());
        finalDetails.put("clientId", effectiveClientId);
        finalDetails.put("leadershipMode", getLeadershipMode());
        if (details != null) {
            finalDetails.putAll(details);
        }
        log.info(ObservabilityEventLogSupport.summary("mqtt_cluster_leadership", result, null, finalDetails));
    }

    private String resolveClientId() {
        return MqttClientIdResolver.resolve(iotProperties.getMqtt().getClientId());
    }

    private MqttConnectOptions buildConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(Boolean.TRUE.equals(iotProperties.getMqtt().getCleanSession()));
        options.setConnectionTimeout(iotProperties.getMqtt().getConnectionTimeout());
        options.setKeepAliveInterval(iotProperties.getMqtt().getKeepAliveInterval());
        if (iotProperties.getMqtt().getUsername() != null) {
            options.setUserName(iotProperties.getMqtt().getUsername());
        }
        if (iotProperties.getMqtt().getPassword() != null) {
            options.setPassword(iotProperties.getMqtt().getPassword().toCharArray());
        }
        return options;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void maybeLogSlowDispatch(String topic,
                                      RawDeviceMessage rawDeviceMessage,
                                      DeviceUpMessage upMessage,
                                      String traceId,
                                      Exception dispatchException,
                                      long costMs) {
        long thresholdMs = resolveSlowMqttThresholdMs();
        if (thresholdMs <= 0 || costMs < thresholdMs || !diagnosticAccessLog.isInfoEnabled()) {
            return;
        }
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", resolveTraceId(traceId, rawDeviceMessage, upMessage));
        details.put("topic", topic);
        details.put("deviceCode", resolveDeviceCode(rawDeviceMessage, upMessage));
        details.put("productKey", resolveProductKey(rawDeviceMessage, upMessage));
        details.put("messageType", resolveMessageType(rawDeviceMessage, upMessage));
        details.put("clientId", rawDeviceMessage == null ? null : rawDeviceMessage.getClientId());
        if (dispatchException != null) {
            details.put("errorClass", dispatchException.getClass().getSimpleName());
        }
        diagnosticAccessLog.info(ObservabilityEventLogSupport.summary(
                "slow_mqtt_dispatch",
                dispatchException == null ? "success" : "failure",
                costMs,
                details
        ));
    }

    private long resolveSlowMqttThresholdMs() {
        IotProperties.Observability observability = iotProperties.getObservability();
        if (observability == null || observability.getPerformance() == null) {
            return 0L;
        }
        Long thresholdMs = observability.getPerformance().getSlowMqttThresholdMs();
        return thresholdMs == null ? 0L : thresholdMs;
    }

    private String resolveTraceId(String traceId, RawDeviceMessage rawDeviceMessage, DeviceUpMessage upMessage) {
        if (upMessage != null && hasText(upMessage.getTraceId())) {
            return upMessage.getTraceId();
        }
        if (rawDeviceMessage != null && hasText(rawDeviceMessage.getTraceId())) {
            return rawDeviceMessage.getTraceId();
        }
        return traceId;
    }

    private String resolveDeviceCode(RawDeviceMessage rawDeviceMessage, DeviceUpMessage upMessage) {
        if (upMessage != null && hasText(upMessage.getDeviceCode())) {
            return upMessage.getDeviceCode();
        }
        return rawDeviceMessage == null ? null : rawDeviceMessage.getDeviceCode();
    }

    private String resolveProductKey(RawDeviceMessage rawDeviceMessage, DeviceUpMessage upMessage) {
        if (upMessage != null && hasText(upMessage.getProductKey())) {
            return upMessage.getProductKey();
        }
        return rawDeviceMessage == null ? null : rawDeviceMessage.getProductKey();
    }

    private String resolveMessageType(RawDeviceMessage rawDeviceMessage, DeviceUpMessage upMessage) {
        if (upMessage != null && hasText(upMessage.getMessageType())) {
            return upMessage.getMessageType();
        }
        return rawDeviceMessage == null ? null : rawDeviceMessage.getMessageType();
    }
}
