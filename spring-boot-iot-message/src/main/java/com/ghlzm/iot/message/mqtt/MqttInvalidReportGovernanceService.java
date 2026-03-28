package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.device.entity.DeviceInvalidReportState;
import com.ghlzm.iot.device.service.DeviceInvalidReportStateService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.invalidreport.InvalidReportCounterStore;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * MQTT 无效上报治理服务。
 */
@Service
public class MqttInvalidReportGovernanceService {

    private static final int MAX_PAYLOAD_CAPTURE_LENGTH = 4000;
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String MQTT_REQUEST_METHOD = "MQTT";
    private static final String EMPTY_PAYLOAD_FINGERPRINT = "empty";

    private final IotProperties iotProperties;
    private final InvalidReportCounterStore invalidReportCounterStore;
    private final DeviceInvalidReportStateService deviceInvalidReportStateService;

    public MqttInvalidReportGovernanceService(IotProperties iotProperties,
                                              InvalidReportCounterStore invalidReportCounterStore,
                                              DeviceInvalidReportStateService deviceInvalidReportStateService) {
        this.iotProperties = iotProperties;
        this.invalidReportCounterStore = invalidReportCounterStore;
        this.deviceInvalidReportStateService = deviceInvalidReportStateService;
    }

    public InvalidMqttReportDecision handleRawEmptyPayload(String topic, RawDeviceMessage rawDeviceMessage) {
        return evaluateAndPersist(
                InvalidMqttReportReason.EMPTY_DECRYPTED_PAYLOAD,
                topic,
                rawDeviceMessage,
                null,
                new IllegalArgumentException("MQTT 负载不能为空")
        );
    }

    public InvalidMqttReportDecision handleDispatchFailure(String topic,
                                                           byte[] payload,
                                                           RawDeviceMessage rawDeviceMessage,
                                                           Throwable throwable) {
        InvalidMqttReportReason reason = resolveReason(throwable);
        if (reason == null) {
            return InvalidMqttReportDecision.allowSample(null);
        }
        return evaluateAndPersist(reason, topic, rawDeviceMessage, payload, throwable);
    }

    private InvalidMqttReportDecision evaluateAndPersist(InvalidMqttReportReason reason,
                                                         String topic,
                                                         RawDeviceMessage rawDeviceMessage,
                                                         byte[] payload,
                                                         Throwable throwable) {
        if (reason == null || !Boolean.TRUE.equals(iotProperties.getObservability().getInvalidReportGovernance().getEnabled())) {
            return InvalidMqttReportDecision.allowSample(reason);
        }

        String failureStage = resolveFailureStage(rawDeviceMessage, throwable, reason);
        if (StringUtils.hasText(failureStage)) {
            invalidReportCounterStore.incrementFailureStage(failureStage);
        }
        invalidReportCounterStore.incrementReasonCode(reason.name());
        GovernancePolicy policy = resolvePolicy(reason);
        String governanceKey = buildGovernanceKey(reason, topic, rawDeviceMessage);
        String scopedReasonKey = buildScopedReasonKey(reason, governanceKey);
        long scopedReasonCount = StringUtils.hasText(scopedReasonKey)
                ? invalidReportCounterStore.incrementReasonCode(scopedReasonKey)
                : 0L;

        boolean thresholdReached = scopedReasonCount >= policy.thresholdCount();
        boolean suppressed = false;
        LocalDateTime suppressedUntil = null;
        if (thresholdReached && StringUtils.hasText(governanceKey)) {
            suppressedUntil = LocalDateTime.now().plusMinutes(policy.cooldownMinutes());
            suppressed = !invalidReportCounterStore.tryOpenCooldown(
                    governanceKey,
                    Duration.ofMinutes(policy.cooldownMinutes())
            );
        }

        persistLatestState(
                governanceKey,
                reason,
                failureStage,
                topic,
                rawDeviceMessage,
                payload,
                throwable,
                suppressed,
                suppressedUntil
        );
        return suppressed
                ? InvalidMqttReportDecision.dropSuppressed(reason)
                : InvalidMqttReportDecision.allowSample(reason);
    }

    private void persistLatestState(String governanceKey,
                                    InvalidMqttReportReason reason,
                                    String failureStage,
                                    String topic,
                                    RawDeviceMessage rawDeviceMessage,
                                    byte[] payload,
                                    Throwable throwable,
                                    boolean suppressed,
                                    LocalDateTime suppressedUntil) {
        if (!StringUtils.hasText(governanceKey)) {
            return;
        }
        DeviceInvalidReportState state = new DeviceInvalidReportState();
        state.setTenantId(resolveTenantId(rawDeviceMessage));
        state.setGovernanceKey(governanceKey);
        state.setReasonCode(reason.name());
        state.setRequestMethod(MQTT_REQUEST_METHOD);
        state.setFailureStage(failureStage);
        state.setDeviceCode(rawDeviceMessage == null ? null : trimToNull(rawDeviceMessage.getDeviceCode()));
        state.setProductKey(rawDeviceMessage == null ? null : trimToNull(rawDeviceMessage.getProductKey()));
        state.setProtocolCode(rawDeviceMessage == null ? null : trimToNull(rawDeviceMessage.getProtocolCode()));
        state.setTopicRouteType(rawDeviceMessage == null ? null : trimToNull(rawDeviceMessage.getTopicRouteType()));
        state.setTopic(trimToNull(topic));
        state.setClientId(rawDeviceMessage == null ? null : trimToNull(rawDeviceMessage.getClientId()));
        state.setPayloadSize(payload == null ? null : payload.length);
        state.setPayloadEncoding(payload == null ? null : "utf-8");
        state.setLastPayload(resolvePayload(payload));
        state.setLastTraceId(resolveTraceId(rawDeviceMessage));
        state.setSampleErrorMessage(throwable == null ? null : trimToNull(throwable.getMessage()));
        state.setSampleExceptionClass(throwable == null ? null : throwable.getClass().getSimpleName());
        state.setHitCount(1L);
        state.setSampledCount(suppressed ? 0L : 1L);
        state.setSuppressedCount(suppressed ? 1L : 0L);
        state.setSuppressedUntil(suppressedUntil);
        deviceInvalidReportStateService.upsertState(state);
    }

    private InvalidMqttReportReason resolveReason(Throwable throwable) {
        String errorText = throwable == null ? "" : (throwable.getClass().getName() + " " + throwable.getMessage()).toLowerCase();
        if (errorText.contains("设备不存在")) {
            return InvalidMqttReportReason.DEVICE_NOT_FOUND;
        }
        if (errorText.contains("mqtt 负载不能为空") || errorText.contains("负载不能为空")) {
            return InvalidMqttReportReason.EMPTY_DECRYPTED_PAYLOAD;
        }
        return null;
    }

    private String resolveFailureStage(RawDeviceMessage rawDeviceMessage,
                                       Throwable throwable,
                                       InvalidMqttReportReason reason) {
        if (InvalidMqttReportReason.EMPTY_DECRYPTED_PAYLOAD.equals(reason)) {
            return "protocol_decode";
        }
        if (rawDeviceMessage == null) {
            return "topic_route";
        }
        String errorText = throwable == null ? "" : (throwable.getClass().getName() + " " + throwable.getMessage()).toLowerCase();
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

    private GovernancePolicy resolvePolicy(InvalidMqttReportReason reason) {
        IotProperties.Observability.InvalidReportGovernance properties =
                iotProperties.getObservability().getInvalidReportGovernance();
        if (InvalidMqttReportReason.DEVICE_NOT_FOUND.equals(reason)) {
            IotProperties.Observability.InvalidReportGovernance.DeviceNotFound deviceNotFound = properties.getDeviceNotFound();
            return new GovernancePolicy(
                    deviceNotFound.getThresholdCount() == null || deviceNotFound.getThresholdCount() < 1 ? 2 : deviceNotFound.getThresholdCount(),
                    deviceNotFound.getCooldownMinutes() == null || deviceNotFound.getCooldownMinutes() < 1 ? 30 : deviceNotFound.getCooldownMinutes()
            );
        }
        IotProperties.Observability.InvalidReportGovernance.EmptyPayload emptyPayload = properties.getEmptyPayload();
        return new GovernancePolicy(
                emptyPayload.getThresholdCount() == null || emptyPayload.getThresholdCount() < 1 ? 3 : emptyPayload.getThresholdCount(),
                emptyPayload.getCooldownMinutes() == null || emptyPayload.getCooldownMinutes() < 1 ? 15 : emptyPayload.getCooldownMinutes()
        );
    }

    private String buildGovernanceKey(InvalidMqttReportReason reason, String topic, RawDeviceMessage rawDeviceMessage) {
        long tenantId = resolveTenantId(rawDeviceMessage);
        String productKey = rawDeviceMessage == null ? null : rawDeviceMessage.getProductKey();
        String deviceCode = rawDeviceMessage == null ? null : rawDeviceMessage.getDeviceCode();
        if (InvalidMqttReportReason.DEVICE_NOT_FOUND.equals(reason)) {
            return "tenant=" + tenantId
                    + "|product=" + normalizeKeySegment(productKey)
                    + "|device=" + normalizeKeySegment(deviceCode)
                    + "|reason=" + reason.name();
        }
        return "tenant=" + tenantId
                + "|topic=" + normalizeKeySegment(topic)
                + "|product=" + normalizeKeySegment(productKey)
                + "|device=" + normalizeKeySegment(deviceCode)
                + "|reason=" + reason.name()
                + "|fingerprint=" + EMPTY_PAYLOAD_FINGERPRINT;
    }

    private String buildScopedReasonKey(InvalidMqttReportReason reason, String governanceKey) {
        if (reason == null || !StringUtils.hasText(governanceKey)) {
            return null;
        }
        return "scoped:" + reason.name() + "|" + governanceKey.trim();
    }

    private long resolveTenantId(RawDeviceMessage rawDeviceMessage) {
        if (rawDeviceMessage != null && StringUtils.hasText(rawDeviceMessage.getTenantId())) {
            try {
                return Long.parseLong(rawDeviceMessage.getTenantId().trim());
            } catch (NumberFormatException ignored) {
                return DEFAULT_TENANT_ID;
            }
        }
        return DEFAULT_TENANT_ID;
    }

    private String resolveTraceId(RawDeviceMessage rawDeviceMessage) {
        if (rawDeviceMessage != null && StringUtils.hasText(rawDeviceMessage.getTraceId())) {
            return rawDeviceMessage.getTraceId().trim();
        }
        return trimToNull(TraceContextHolder.getTraceId());
    }

    private String resolvePayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        String value = new String(payload, StandardCharsets.UTF_8);
        if (value.length() <= MAX_PAYLOAD_CAPTURE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_PAYLOAD_CAPTURE_LENGTH);
    }

    private String normalizeKeySegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        return value.trim().replace('|', '_');
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record GovernancePolicy(int thresholdCount, int cooldownMinutes) {
    }
}
