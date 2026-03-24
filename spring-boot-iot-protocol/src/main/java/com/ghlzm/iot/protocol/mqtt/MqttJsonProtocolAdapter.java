package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpChildMessageSplitter;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpEnvelopeDecoder;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpFamilyResolver;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpNormalizeResult;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpPropertyNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MQTT JSON 协议适配器。
 * 一期通过该适配器把 HTTP 模拟上报中的 JSON payload 转换成统一上行消息。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Component
public class MqttJsonProtocolAdapter implements ProtocolAdapter {

    private static final Logger log = LoggerFactory.getLogger(MqttJsonProtocolAdapter.class);

    private static final List<String> PRODUCT_KEY_ALIASES = List.of(
            "productKey", "product_code", "productCode", "product_key", "pk"
    );
    private static final List<String> DEVICE_CODE_ALIASES = List.of(
            "deviceCode", "device_code", "deviceId", "device_id", "devId", "dev_id", "imei", "sn", "did"
    );
    private static final List<String> LEGACY_STATUS_FIELD_ALIASES = List.of(
            "ext_power_volt", "solar_volt", "battery_dump_energy", "signal_4g", "sensor_state", "lon", "lat"
    );
    private static final List<String> PROPERTY_CONTAINER_ALIASES = List.of(
            "properties", "property", "data", "params", "reported"
    );
    private static final List<String> RESERVED_PROPERTY_KEYS = List.of(
            "messageType", "productKey", "product_code", "productCode", "product_key", "pk",
            "deviceCode", "device_code", "deviceId", "device_id", "devId", "dev_id", "imei", "sn",
            "topic", "clientId", "client_id", "timestamp", "ts", "header", "headers", "body", "bodies",
            "_dataFormatType", "_fileStreamLength", "_fileStreamBase64", "_firmwarePacket", "_binaryLength"
    );

    /**
     * 当前阶段协议层只需要最小 JSON 解析能力，直接在适配器内部维护 ObjectMapper，
     * 避免联调测试依赖额外的 Jackson Bean 注入条件。
     */
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry;
    private final MqttPayloadFrameParser mqttPayloadFrameParser;
    private final MqttPayloadSecurityValidator mqttPayloadSecurityValidator;
    private final MqttFirmwarePacketParser mqttFirmwarePacketParser;
    private final IotProperties iotProperties;
    private final LegacyDpEnvelopeDecoder legacyDpEnvelopeDecoder;
    private final LegacyDpFamilyResolver legacyDpFamilyResolver;
    private final LegacyDpPropertyNormalizer legacyDpPropertyNormalizer;
    private final LegacyDpChildMessageSplitter legacyDpChildMessageSplitter;

    @Autowired
    public MqttJsonProtocolAdapter(MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry,
                                   MqttPayloadFrameParser mqttPayloadFrameParser,
                                   MqttPayloadSecurityValidator mqttPayloadSecurityValidator,
                                   MqttFirmwarePacketParser mqttFirmwarePacketParser,
                                   IotProperties iotProperties) {
        this(
                mqttPayloadDecryptorRegistry,
                mqttPayloadFrameParser,
                mqttPayloadSecurityValidator,
                mqttFirmwarePacketParser,
                iotProperties,
                new LegacyDpFamilyResolver(),
                new LegacyDpPropertyNormalizer(),
                new LegacyDpChildMessageSplitter()
        );
    }

    MqttJsonProtocolAdapter(MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry,
                            MqttPayloadFrameParser mqttPayloadFrameParser,
                            MqttPayloadSecurityValidator mqttPayloadSecurityValidator,
                            MqttFirmwarePacketParser mqttFirmwarePacketParser,
                            IotProperties iotProperties,
                            LegacyDpFamilyResolver legacyDpFamilyResolver,
                            LegacyDpPropertyNormalizer legacyDpPropertyNormalizer,
                            LegacyDpChildMessageSplitter legacyDpChildMessageSplitter) {
        this.mqttPayloadDecryptorRegistry = mqttPayloadDecryptorRegistry;
        this.mqttPayloadFrameParser = mqttPayloadFrameParser;
        this.mqttPayloadSecurityValidator = mqttPayloadSecurityValidator;
        this.mqttFirmwarePacketParser = mqttFirmwarePacketParser;
        this.iotProperties = iotProperties;
        this.legacyDpEnvelopeDecoder = new LegacyDpEnvelopeDecoder(
                mqttPayloadDecryptorRegistry,
                mqttPayloadFrameParser,
                mqttPayloadSecurityValidator,
                mqttFirmwarePacketParser
        );
        this.legacyDpFamilyResolver = legacyDpFamilyResolver;
        this.legacyDpPropertyNormalizer = legacyDpPropertyNormalizer;
        this.legacyDpChildMessageSplitter = legacyDpChildMessageSplitter;
    }

    @Override
    public String getProtocolCode() {
        return "mqtt-json";
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeviceUpMessage decode(byte[] payload, ProtocolContext context) {
        try {
            LegacyDpEnvelopeDecoder.DecodedPayload decodedPayload = legacyDpEnvelopeDecoder.decode(payload);
            Map<String, Object> map = decodedPayload.payload();
            DeviceUpMessage message = new DeviceUpMessage();
            message.setTenantId(context.getTenantCode());
            // 标准 topic 优先，历史兼容 topic 再回退到 payload 字段或设备报文根节点提取。
            String resolvedDeviceCode = resolveDeviceCode(context, map);
            boolean legacyDp = isLegacyDp(context);
            List<String> familyCodes = List.of();
            message.setProductKey(resolveIdentity(context.getProductKey(), map, PRODUCT_KEY_ALIASES));
            message.setDeviceCode(resolvedDeviceCode);
            LegacyDpNormalizeResult normalizeResult = null;
            LegacyDpNormalizeResult legacyV1NormalizeResult = null;
            LegacyDpNormalizeResult legacyV2NormalizeResult = null;
            TimestampResolution timestampResolution;
            boolean childSplitApplied = false;
            if (decodedPayload.dataFormatType() != MqttDataFormatType.STANDARD_TYPE_3) {
                if (legacyDp) {
                    if (shouldUseLegacyDpV1Path()) {
                        legacyV1NormalizeResult = buildLegacyV1NormalizeResult(map, resolvedDeviceCode);
                    }
                    if (shouldEvaluateLegacyDpV2Path()) {
                        legacyV2NormalizeResult = buildLegacyV2NormalizeResult(map, resolvedDeviceCode);
                    }
                    normalizeResult = selectLegacyNormalizeResult(legacyV1NormalizeResult, legacyV2NormalizeResult);
                } else {
                    familyCodes = legacyDpFamilyResolver.resolveFamilyCodes(map, resolvedDeviceCode);
                    normalizeResult = legacyDpPropertyNormalizer.normalize(map, resolvedDeviceCode, familyCodes);
                }
                familyCodes = normalizeResult == null || normalizeResult.familyCodes() == null
                        ? List.of()
                        : normalizeResult.familyCodes();
                timestampResolution = new TimestampResolution(
                        normalizeResult.timestamp(),
                        normalizeResult.timestampSource()
                );
            } else {
                familyCodes = legacyDpFamilyResolver.resolveFamilyCodes(map, resolvedDeviceCode);
                timestampResolution = resolveTimestamp(map, resolvedDeviceCode);
            }
            // MQTT 场景优先使用 topic 解析出的 messageType，payload 中的 messageType 作为回退信息。
            String payloadMessageType = stringValue(map.get("messageType"));
            message.setMessageType(resolveMessageType(
                    context,
                    payloadMessageType,
                    map,
                    decodedPayload.dataFormatType(),
                    normalizeResult == null ? null : normalizeResult.messageType()
            ));
            message.setTopic(context.getTopic());
            message.setDataFormatType(decodedPayload.dataFormatType() == null
                    ? null
                    : decodedPayload.dataFormatType().name());
            message.setTimestamp(timestampResolution.timestamp());

            if (decodedPayload.dataFormatType() == MqttDataFormatType.STANDARD_TYPE_3) {
                // 表 C.3 / C.4 属于文件或升级分包类消息，当前先收口到事件元数据，
                // 不进入一期最新属性表，避免把文件描述字段误写成设备属性。
                message.setFilePayload(decodedPayload.filePayload());
                message.setEvents(buildFileEvents(map));
            } else {
                Map<String, Object> properties = normalizeResult == null
                        ? Map.of()
                        : normalizeResult.properties();
                SplitExecutionResult splitResult;
                if (legacyDp) {
                    SplitExecutionResult legacyV1SplitResult = shouldUseLegacyDpV1Path()
                            ? buildLegacyV1SplitExecutionResult(
                            map,
                            resolvedDeviceCode,
                            message.getTenantId(),
                            message.getProductKey(),
                            message.getMessageType(),
                            message.getTopic(),
                            timestampResolution.timestamp()
                    )
                            : null;
                    SplitExecutionResult legacyV2SplitResult = shouldEvaluateLegacyDpV2Path()
                            ? buildLegacyV2SplitExecutionResult(
                            map,
                            resolvedDeviceCode,
                            message.getTenantId(),
                            message.getProductKey(),
                            message.getMessageType(),
                            message.getTopic(),
                            timestampResolution.timestamp()
                    )
                            : null;
                    if (isLegacyDpValidateOnlyEnabled()) {
                        maybeLogLegacyDpValidationDiff(
                                context,
                                resolvedDeviceCode,
                                legacyV1NormalizeResult,
                                legacyV2NormalizeResult,
                                legacyV1SplitResult,
                                legacyV2SplitResult
                        );
                    }
                    splitResult = selectLegacySplitResult(legacyV1SplitResult, legacyV2SplitResult);
                } else {
                    splitResult = buildLegacyV2SplitExecutionResult(
                            map,
                            resolvedDeviceCode,
                            message.getTenantId(),
                            message.getProductKey(),
                            message.getMessageType(),
                            message.getTopic(),
                            timestampResolution.timestamp()
                    );
                }
                if (!splitResult.messages().isEmpty()) {
                    properties = removeChildLogicalProperties(properties, splitResult.logicalCodes());
                    message.setChildMessages(splitResult.messages());
                    childSplitApplied = true;
                }
                if (!properties.isEmpty()) {
                    message.setProperties(properties);
                }
            }

            Object events = map.get("events");
            if (events instanceof Map<?, ?>) {
                message.setEvents((Map<String, Object>) events);
            }

            DeviceUpProtocolMetadata protocolMetadata = buildProtocolMetadata(
                    context,
                    decodedPayload,
                    map,
                    resolvedDeviceCode,
                    familyCodes,
                    timestampResolution.source(),
                    childSplitApplied
            );
            if (protocolMetadata != null) {
                message.setProtocolMetadata(protocolMetadata);
            }
            message.setRawPayload(decodedPayload.rawPayload());
            return message;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception e) {
            throw new BizException(500, buildDecodeFailureMessage(e), e);
        }
    }

    @Override
    public byte[] encode(DeviceDownMessage message, ProtocolContext context) {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (Exception e) {
            throw new BizException("MQTT JSON 协议编码失败");
        }
    }

    private boolean isLegacyDp(ProtocolContext context) {
        return context != null
                && ("$dp".equals(context.getTopic())
                || "legacy".equalsIgnoreCase(context.getTopicRouteType()));
    }

    private boolean shouldUseLegacyDpV1Path() {
        return isLegacyDpValidateOnlyEnabled() || !isLegacyDpNormalizerV2Enabled();
    }

    private boolean shouldEvaluateLegacyDpV2Path() {
        return isLegacyDpValidateOnlyEnabled() || isLegacyDpNormalizerV2Enabled();
    }

    private boolean isLegacyDpNormalizerV2Enabled() {
        IotProperties.Protocol protocol = iotProperties == null ? null : iotProperties.getProtocol();
        IotProperties.Protocol.LegacyDp legacyDp = protocol == null ? null : protocol.getLegacyDp();
        return legacyDp != null && Boolean.TRUE.equals(legacyDp.getNormalizerV2Enabled());
    }

    private boolean isLegacyDpFamilyObservabilityEnabled() {
        IotProperties.Protocol protocol = iotProperties == null ? null : iotProperties.getProtocol();
        IotProperties.Protocol.LegacyDp legacyDp = protocol == null ? null : protocol.getLegacyDp();
        return legacyDp == null || !Boolean.FALSE.equals(legacyDp.getFamilyObservabilityEnabled());
    }

    private boolean isLegacyDpValidateOnlyEnabled() {
        IotProperties.Telemetry telemetry = iotProperties == null ? null : iotProperties.getTelemetry();
        return telemetry != null && Boolean.TRUE.equals(telemetry.getLegacyMappingValidateOnly());
    }

    private LegacyDpNormalizeResult buildLegacyV1NormalizeResult(Map<String, Object> payload,
                                                                 String resolvedDeviceCode) {
        List<String> familyCodes = resolveFamilyCodes(payload, resolvedDeviceCode);
        Map<String, Object> properties = resolveProperties(payload, resolvedDeviceCode);
        TimestampResolution timestampResolution = resolveTimestamp(payload, resolvedDeviceCode);
        return new LegacyDpNormalizeResult(
                familyCodes,
                properties,
                inferLegacyMessageType(payload, resolvedDeviceCode),
                timestampResolution.timestamp(),
                timestampResolution.source()
        );
    }

    private LegacyDpNormalizeResult buildLegacyV2NormalizeResult(Map<String, Object> payload,
                                                                 String resolvedDeviceCode) {
        List<String> familyCodes = legacyDpFamilyResolver.resolveFamilyCodes(payload, resolvedDeviceCode);
        return legacyDpPropertyNormalizer.normalize(payload, resolvedDeviceCode, familyCodes);
    }

    private LegacyDpNormalizeResult selectLegacyNormalizeResult(LegacyDpNormalizeResult legacyV1NormalizeResult,
                                                                LegacyDpNormalizeResult legacyV2NormalizeResult) {
        if (isLegacyDpValidateOnlyEnabled()) {
            return legacyV1NormalizeResult;
        }
        if (isLegacyDpNormalizerV2Enabled()) {
            return legacyV2NormalizeResult;
        }
        return legacyV1NormalizeResult;
    }

    private SplitExecutionResult buildLegacyV1SplitExecutionResult(Map<String, Object> payload,
                                                                   String resolvedDeviceCode,
                                                                   String tenantId,
                                                                   String productKey,
                                                                   String messageType,
                                                                   String topic,
                                                                   LocalDateTime fallbackTimestamp) {
        ConfiguredChildMessages configuredChildMessages = buildConfiguredChildMessages(
                payload,
                resolvedDeviceCode,
                tenantId,
                productKey,
                messageType,
                topic,
                fallbackTimestamp
        );
        return new SplitExecutionResult(configuredChildMessages.messages(), configuredChildMessages.logicalCodes());
    }

    private SplitExecutionResult buildLegacyV2SplitExecutionResult(Map<String, Object> payload,
                                                                   String resolvedDeviceCode,
                                                                   String tenantId,
                                                                   String productKey,
                                                                   String messageType,
                                                                   String topic,
                                                                   LocalDateTime fallbackTimestamp) {
        LegacyDpChildMessageSplitter.SplitResult splitResult = legacyDpChildMessageSplitter.split(
                payload,
                resolvedDeviceCode,
                tenantId,
                productKey,
                messageType,
                topic,
                fallbackTimestamp,
                resolveSubDeviceMappings(resolvedDeviceCode)
        );
        return new SplitExecutionResult(splitResult.messages(), splitResult.logicalCodes());
    }

    private SplitExecutionResult selectLegacySplitResult(SplitExecutionResult legacyV1SplitResult,
                                                         SplitExecutionResult legacyV2SplitResult) {
        if (isLegacyDpValidateOnlyEnabled()) {
            return legacyV1SplitResult == null ? SplitExecutionResult.empty() : legacyV1SplitResult;
        }
        if (isLegacyDpNormalizerV2Enabled()) {
            return legacyV2SplitResult == null ? SplitExecutionResult.empty() : legacyV2SplitResult;
        }
        return legacyV1SplitResult == null ? SplitExecutionResult.empty() : legacyV1SplitResult;
    }

    private void maybeLogLegacyDpValidationDiff(ProtocolContext context,
                                                String resolvedDeviceCode,
                                                LegacyDpNormalizeResult legacyV1NormalizeResult,
                                                LegacyDpNormalizeResult legacyV2NormalizeResult,
                                                SplitExecutionResult legacyV1SplitResult,
                                                SplitExecutionResult legacyV2SplitResult) {
        if (legacyV1NormalizeResult == null || legacyV2NormalizeResult == null) {
            return;
        }
        List<String> diffFields = new ArrayList<>();
        if (!Objects.equals(legacyV1NormalizeResult.familyCodes(), legacyV2NormalizeResult.familyCodes())) {
            diffFields.add("familyCodes");
        }
        if (!Objects.equals(legacyV1NormalizeResult.properties(), legacyV2NormalizeResult.properties())) {
            diffFields.add("properties");
        }
        if (!Objects.equals(legacyV1NormalizeResult.messageType(), legacyV2NormalizeResult.messageType())) {
            diffFields.add("messageType");
        }
        if (!Objects.equals(legacyV1NormalizeResult.timestampSource(), legacyV2NormalizeResult.timestampSource())) {
            diffFields.add("timestampSource");
        }
        if (!sameSplitResult(legacyV1SplitResult, legacyV2SplitResult)) {
            diffFields.add("childMessages");
        }
        if (diffFields.isEmpty()) {
            return;
        }
        log.warn(
                "event=\"legacy_dp_normalizer_validation_diff\" topic=\"{}\" routeType=\"{}\" deviceCode=\"{}\" diffFields=\"{}\" v1Families=\"{}\" v2Families=\"{}\"",
                context == null ? null : context.getTopic(),
                context == null ? null : context.getTopicRouteType(),
                resolvedDeviceCode,
                String.join(",", diffFields),
                legacyV1NormalizeResult.familyCodes(),
                legacyV2NormalizeResult.familyCodes()
        );
    }

    private boolean sameSplitResult(SplitExecutionResult left, SplitExecutionResult right) {
        SplitExecutionResult safeLeft = left == null ? SplitExecutionResult.empty() : left;
        SplitExecutionResult safeRight = right == null ? SplitExecutionResult.empty() : right;
        if (!Objects.equals(safeLeft.logicalCodes(), safeRight.logicalCodes())) {
            return false;
        }
        if (safeLeft.messages().size() != safeRight.messages().size()) {
            return false;
        }
        for (int index = 0; index < safeLeft.messages().size(); index++) {
            DeviceUpMessage leftMessage = safeLeft.messages().get(index);
            DeviceUpMessage rightMessage = safeRight.messages().get(index);
            if (!Objects.equals(leftMessage.getDeviceCode(), rightMessage.getDeviceCode())
                    || !Objects.equals(leftMessage.getProperties(), rightMessage.getProperties())) {
                return false;
            }
        }
        return true;
    }

    private String resolveMessageType(ProtocolContext context,
                                      String payloadMessageType,
                                      Map<String, Object> payload,
                                      MqttDataFormatType dataFormatType,
                                      String normalizedMessageType) {
        if (dataFormatType == MqttDataFormatType.STANDARD_TYPE_3) {
            return inferFileMessageType(payload);
        }
        if ("$dp".equals(context.getTopic())) {
            if (normalizedMessageType != null && !normalizedMessageType.isBlank()) {
                return normalizedMessageType;
            }
        }
        if (context.getMessageType() != null && !context.getMessageType().isBlank()) {
            return context.getMessageType();
        }
        if (payloadMessageType != null && !payloadMessageType.isBlank()) {
            return payloadMessageType;
        }
        return "property";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveIdentity(String contextValue, Map<String, Object> payload, List<String> aliases) {
        if (contextValue != null && !contextValue.isBlank()) {
            return contextValue;
        }
        for (String alias : aliases) {
            String value = stringValue(payload.get(alias));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String resolveDeviceCode(ProtocolContext context, Map<String, Object> payload) {
        String resolvedFromStandardFields = resolveIdentity(context.getDeviceCode(), payload, DEVICE_CODE_ALIASES);
        if (resolvedFromStandardFields != null && !resolvedFromStandardFields.isBlank()) {
            return resolvedFromStandardFields;
        }

        // 兼容历史平台的 $dp 报文：设备编码可能直接作为最外层 key。
        List<String> candidates = topLevelDataKeys(payload);
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveProperties(Map<String, Object> payload, String resolvedDeviceCode) {
        for (String alias : PROPERTY_CONTAINER_ALIASES) {
            Object value = payload.get(alias);
            if (value instanceof Map<?, ?> mapValue) {
                return (Map<String, Object>) mapValue;
            }
        }

        Object legacyBody = payload;
        if (resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload) {
            legacyBody = devicePayload;
        }

        if (legacyBody instanceof Map<?, ?> legacyMap) {
            Map<String, Object> flattened = new LinkedHashMap<>();
            flattenLegacyProperties("", legacyMap, flattened);
            if (!flattened.isEmpty()) {
                return flattened;
            }
        }

        // 兼容更简单的旧报文：如果没有标准 properties 容器，就把顶层业务字段视为属性集合。
        Map<String, Object> fallback = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (RESERVED_PROPERTY_KEYS.contains(entry.getKey())) {
                continue;
            }
            fallback.put(entry.getKey(), entry.getValue());
        }
        return fallback;
    }

    private void flattenLegacyProperties(String prefix, Map<?, ?> source, Map<String, Object> target) {
        if (isTimestampContainer(source)) {
            List<Map.Entry<String, ?>> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    entries.add(Map.entry(key, entry.getValue()));
                }
            }
            entries.sort(Comparator.comparing(Map.Entry::getKey));
            if (!entries.isEmpty()) {
                Object latestValue = entries.get(entries.size() - 1).getValue();
                if (latestValue instanceof Map<?, ?> latestMap) {
                    flattenLegacyProperties(prefix, latestMap, target);
                } else if (prefix != null && !prefix.isBlank()) {
                    target.put(prefix, latestValue);
                }
            }
            return;
        }

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            if ((prefix == null || prefix.isBlank()) && RESERVED_PROPERTY_KEYS.contains(key)) {
                continue;
            }
            String field = prefix == null || prefix.isBlank() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                flattenLegacyProperties(field, nestedMap, target);
                continue;
            }
            target.put(field, value);
        }
    }

    private boolean isTimestampContainer(Map<?, ?> source) {
        if (source.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !isTimestampKey(key)) {
                return false;
            }
        }
        return true;
    }

    private TimestampResolution resolveTimestamp(Map<String, Object> payload, String resolvedDeviceCode) {
        List<LocalDateTime> timestamps = new ArrayList<>();
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        collectTimestamps(body, timestamps);
        if (!timestamps.isEmpty()) {
            timestamps.sort(LocalDateTime::compareTo);
            return new TimestampResolution(timestamps.get(timestamps.size() - 1), "PAYLOAD_LATEST_TIMESTAMP");
        }
        return new TimestampResolution(LocalDateTime.now(), "SERVER_NOW");
    }

    private void collectTimestamps(Object source, List<LocalDateTime> timestamps) {
        if (!(source instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && isTimestampKey(key)) {
                LocalDateTime parsed = parseTimestamp(key);
                if (parsed != null) {
                    timestamps.add(parsed);
                }
            }
            if (entry.getKey() instanceof String key
                    && ("at".equalsIgnoreCase(key) || "timestamp".equalsIgnoreCase(key) || "ts".equalsIgnoreCase(key))
                    && entry.getValue() != null) {
                LocalDateTime parsed = parseTimestamp(String.valueOf(entry.getValue()));
                if (parsed != null) {
                    timestamps.add(parsed);
                }
            }
            collectTimestamps(entry.getValue(), timestamps);
        }
    }

    private boolean isTimestampKey(String key) {
        return parseTimestamp(key) != null;
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                long epochMillis = Long.parseLong(value);
                return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (NumberFormatException numberFormatException) {
                return null;
            }
        }
    }

    private String inferLegacyMessageType(Map<String, Object> payload, String resolvedDeviceCode) {
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        if (!(body instanceof Map<?, ?> bodyMap)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : bodyMap.entrySet()) {
            if (entry.getKey() instanceof String key && key.contains("_ZT_")) {
                return "status";
            }
        }

        for (String field : LEGACY_STATUS_FIELD_ALIASES) {
            if (containsField(bodyMap, field)) {
                return "status";
            }
        }
        return "property";
    }

    private boolean containsField(Map<?, ?> source, String expectedField) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (expectedField.equals(entry.getKey())) {
                return true;
            }
            if (entry.getValue() instanceof Map<?, ?> nestedMap && containsField(nestedMap, expectedField)) {
                return true;
            }
        }
        return false;
    }

    private ConfiguredChildMessages buildConfiguredChildMessages(Map<String, Object> payload,
                                                                 String baseDeviceCode,
                                                                 String tenantId,
                                                                 String productKey,
                                                                 String messageType,
                                                                 String topic,
                                                                 LocalDateTime fallbackTimestamp) {
        Map<String, String> subDeviceMappings = resolveSubDeviceMappings(baseDeviceCode);
        if (subDeviceMappings.isEmpty()) {
            return ConfiguredChildMessages.empty();
        }
        Object basePayload = baseDeviceCode == null ? null : payload.get(baseDeviceCode);
        if (!(basePayload instanceof Map<?, ?> basePayloadMap)) {
            return ConfiguredChildMessages.empty();
        }

        List<DeviceUpMessage> childMessages = new ArrayList<>();
        List<String> logicalCodes = new ArrayList<>();
        for (Map.Entry<String, String> entry : subDeviceMappings.entrySet()) {
            String logicalCode = entry.getKey();
            String childDeviceCode = entry.getValue();
            if (logicalCode == null || logicalCode.isBlank() || childDeviceCode == null || childDeviceCode.isBlank()) {
                continue;
            }

            LatestLogicalPayload latestLogicalPayload = extractLatestLogicalPayload(logicalCode, basePayloadMap.get(logicalCode));
            if (latestLogicalPayload == null || latestLogicalPayload.properties().isEmpty()) {
                continue;
            }

            DeviceUpMessage childMessage = new DeviceUpMessage();
            childMessage.setTenantId(tenantId);
            childMessage.setProductKey(productKey);
            childMessage.setDeviceCode(childDeviceCode);
            childMessage.setMessageType(messageType);
            childMessage.setTopic(topic);
            childMessage.setTimestamp(latestLogicalPayload.timestamp() == null ? fallbackTimestamp : latestLogicalPayload.timestamp());
            childMessage.setProperties(latestLogicalPayload.properties());
            childMessage.setRawPayload(latestLogicalPayload.rawPayload());
            childMessages.add(childMessage);
            logicalCodes.add(logicalCode);
        }
        return new ConfiguredChildMessages(childMessages, logicalCodes);
    }

    private Map<String, String> resolveSubDeviceMappings(String baseDeviceCode) {
        if (baseDeviceCode == null || baseDeviceCode.isBlank() || iotProperties.getDevice() == null
                || iotProperties.getDevice().getSubDeviceMappings() == null) {
            return Map.of();
        }
        Map<String, String> configuredMappings = iotProperties.getDevice().getSubDeviceMappings().get(baseDeviceCode);
        return configuredMappings == null || configuredMappings.isEmpty() ? Map.of() : configuredMappings;
    }

    private LatestLogicalPayload extractLatestLogicalPayload(String logicalCode, Object logicalPayload) {
        if (logicalPayload == null) {
            return null;
        }

        LocalDateTime logicalTimestamp = null;
        Object latestValue = logicalPayload;
        String rawPayload = writeLogicalRawPayload(logicalCode, logicalPayload);
        if (logicalPayload instanceof Map<?, ?> logicalMap && isTimestampContainer(logicalMap)) {
            TimestampedValue latestEntry = selectLatestTimestampValue(logicalMap);
            if (latestEntry == null) {
                return null;
            }
            logicalTimestamp = latestEntry.timestamp();
            latestValue = latestEntry.value();
            Map<String, Object> latestPayload = new LinkedHashMap<>();
            latestPayload.put(latestEntry.key(), latestEntry.value());
            rawPayload = writeLogicalRawPayload(logicalCode, latestPayload);
        }

        Map<String, Object> properties = toLogicalProperties(logicalCode, latestValue);
        return properties.isEmpty() ? null : new LatestLogicalPayload(logicalTimestamp, properties, rawPayload);
    }

    private TimestampedValue selectLatestTimestampValue(Map<?, ?> source) {
        List<TimestampedValue> timestampedValues = new ArrayList<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            LocalDateTime timestamp = parseTimestamp(key);
            if (timestamp != null) {
                timestampedValues.add(new TimestampedValue(key, timestamp, entry.getValue()));
            }
        }
        if (timestampedValues.isEmpty()) {
            return null;
        }
        timestampedValues.sort(Comparator.comparing(TimestampedValue::timestamp).thenComparing(TimestampedValue::key));
        return timestampedValues.get(timestampedValues.size() - 1);
    }

    private Map<String, Object> toLogicalProperties(String logicalCode, Object value) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> mapValue) {
            flattenChildProperties("", mapValue, properties);
            return properties;
        }
        if (value != null) {
            properties.put(logicalCode, value);
        }
        return properties;
    }

    private void flattenChildProperties(String prefix, Map<?, ?> source, Map<String, Object> target) {
        if (isTimestampContainer(source)) {
            TimestampedValue latestEntry = selectLatestTimestampValue(source);
            if (latestEntry == null) {
                return;
            }
            Object latestValue = latestEntry.value();
            if (latestValue instanceof Map<?, ?> latestMap) {
                flattenChildProperties(prefix, latestMap, target);
            } else if (prefix != null && !prefix.isBlank()) {
                target.put(prefix, latestValue);
            }
            return;
        }

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            String field = prefix == null || prefix.isBlank() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                flattenChildProperties(field, nestedMap, target);
                continue;
            }
            target.put(field, value);
        }
    }

    private String writeLogicalRawPayload(String logicalCode, Object logicalPayload) {
        try {
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put(logicalCode, logicalPayload);
            return objectMapper.writeValueAsString(rawPayload);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> removeChildLogicalProperties(Map<String, Object> properties, List<String> logicalCodes) {
        if (properties == null || properties.isEmpty() || logicalCodes == null || logicalCodes.isEmpty()) {
            return properties;
        }
        Map<String, Object> filteredProperties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (isChildLogicalProperty(entry.getKey(), logicalCodes)) {
                continue;
            }
            filteredProperties.put(entry.getKey(), entry.getValue());
        }
        return filteredProperties;
    }

    private boolean isChildLogicalProperty(String propertyKey, List<String> logicalCodes) {
        if (propertyKey == null || propertyKey.isBlank()) {
            return false;
        }
        for (String logicalCode : logicalCodes) {
            if (logicalCode != null && !logicalCode.isBlank()
                    && (propertyKey.equals(logicalCode) || propertyKey.startsWith(logicalCode + "."))) {
                return true;
            }
        }
        return false;
    }

    private List<String> topLevelDataKeys(Map<String, Object> payload) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (RESERVED_PROPERTY_KEYS.contains(entry.getKey())) {
                continue;
            }
            if (entry.getValue() instanceof Map<?, ?>) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    private DeviceUpProtocolMetadata buildProtocolMetadata(ProtocolContext context,
                                                           LegacyDpEnvelopeDecoder.DecodedPayload decodedPayload,
                                                           Map<String, Object> payload,
                                                           String resolvedDeviceCode,
                                                           List<String> familyCodes,
                                                           String timestampSource,
                                                           boolean childSplitApplied) {
        boolean legacyDp = isLegacyDp(context);
        if (legacyDp && !isLegacyDpFamilyObservabilityEnabled()) {
            return null;
        }
        if (!legacyDp && decodedPayload.appId() == null) {
            return null;
        }

        DeviceUpProtocolMetadata metadata = new DeviceUpProtocolMetadata();
        metadata.setAppId(decodedPayload.appId());
        metadata.setFamilyCodes(familyCodes == null ? List.of() : familyCodes);
        metadata.setNormalizationStrategy(legacyDp ? "LEGACY_DP" : "DIRECT_JSON");
        metadata.setTimestampSource(timestampSource);
        metadata.setChildSplitApplied(childSplitApplied);
        metadata.setRouteType(context == null ? null : context.getTopicRouteType());
        return metadata;
    }

    private List<String> resolveFamilyCodes(Map<String, Object> payload, String resolvedDeviceCode) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> familyCodes = new LinkedHashSet<>();
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        if (body instanceof Map<?, ?> bodyMap) {
            for (Map.Entry<?, ?> entry : bodyMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                if (RESERVED_PROPERTY_KEYS.contains(key)) {
                    continue;
                }
                familyCodes.add(key);
            }
        }
        if (familyCodes.isEmpty()) {
            familyCodes.addAll(topLevelDataKeys(payload));
        }
        return familyCodes.isEmpty() ? List.of() : List.copyOf(familyCodes);
    }

    private String inferFileMessageType(Map<String, Object> payload) {
        Object fileType = payload.get("file_type");
        if (fileType != null) {
            String normalized = String.valueOf(fileType).trim().toLowerCase();
            if ("bin".equals(normalized) || "firmware".equals(normalized) || "ota".equals(normalized)) {
                return "firmware";
            }
        }
        return "file";
    }

    private Map<String, Object> buildFileEvents(Map<String, Object> payload) {
        Map<String, Object> events = new LinkedHashMap<>();
        Map<String, Object> fileMetadata = new LinkedHashMap<>(payload);
        fileMetadata.remove("_fileStreamBase64");
        events.put("file", fileMetadata);
        return events;
    }

    private String buildDecodeFailureMessage(Exception exception) {
        String detail = exception == null ? null : exception.getMessage();
        if (detail == null || detail.isBlank()) {
            return "MQTT JSON 协议解析失败";
        }
        return "MQTT JSON 协议解析失败: " + detail;
    }

    private record TimestampResolution(LocalDateTime timestamp,
                                       String source) {
    }

    private record LatestLogicalPayload(LocalDateTime timestamp,
                                        Map<String, Object> properties,
                                        String rawPayload) {
    }

    private record TimestampedValue(String key,
                                    LocalDateTime timestamp,
                                    Object value) {
    }

    private record SplitExecutionResult(List<DeviceUpMessage> messages,
                                        List<String> logicalCodes) {

        private static SplitExecutionResult empty() {
            return new SplitExecutionResult(List.of(), List.of());
        }
    }

    private record ConfiguredChildMessages(List<DeviceUpMessage> messages,
                                           List<String> logicalCodes) {

        private static ConfiguredChildMessages empty() {
            return new ConfiguredChildMessages(List.of(), List.of());
        }
    }
}
