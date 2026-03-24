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
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final List<String> PRODUCT_KEY_ALIASES = List.of(
            "productKey", "product_code", "productCode", "product_key", "pk"
    );
    private static final List<String> DEVICE_CODE_ALIASES = List.of(
            "deviceCode", "device_code", "deviceId", "device_id", "devId", "dev_id", "imei", "sn", "did"
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
    private final LegacyDpEnvelopeDecoder legacyDpEnvelopeDecoder;
    private final LegacyDpFamilyResolver legacyDpFamilyResolver;
    private final LegacyDpPropertyNormalizer legacyDpPropertyNormalizer;
    private final LegacyDpChildMessageSplitter legacyDpChildMessageSplitter;
    private final IotProperties iotProperties;

    public MqttJsonProtocolAdapter(LegacyDpEnvelopeDecoder legacyDpEnvelopeDecoder,
                                   IotProperties iotProperties) {
        this.legacyDpEnvelopeDecoder = legacyDpEnvelopeDecoder;
        this.iotProperties = iotProperties;
        this.legacyDpFamilyResolver = new LegacyDpFamilyResolver();
        this.legacyDpPropertyNormalizer = new LegacyDpPropertyNormalizer(this.legacyDpFamilyResolver);
        this.legacyDpChildMessageSplitter = new LegacyDpChildMessageSplitter(iotProperties);
    }

    @Override
    public String getProtocolCode() {
        return "mqtt-json";
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeviceUpMessage decode(byte[] payload, ProtocolContext context) {
        try {
            LegacyDpEnvelopeDecoder.DecodedEnvelope decodedPayload = legacyDpEnvelopeDecoder.decode(payload);
            Map<String, Object> map = decodedPayload.payload();
            DeviceUpMessage message = new DeviceUpMessage();
            message.setTenantId(context.getTenantCode());
            // 标准 topic 优先，历史兼容 topic 再回退到 payload 字段或设备报文根节点提取。
            String resolvedDeviceCode = resolveDeviceCode(context, map);
            message.setProductKey(resolveIdentity(context.getProductKey(), map, PRODUCT_KEY_ALIASES));
            message.setDeviceCode(resolvedDeviceCode);
            // MQTT 场景优先使用 topic 解析出的 messageType，payload 中的 messageType 作为回退信息。
            String payloadMessageType = stringValue(map.get("messageType"));
            message.setTopic(context.getTopic());
            message.setDataFormatType(decodedPayload.dataFormatType() == null
                    ? null
                    : decodedPayload.dataFormatType().name());
            LegacyDpNormalizeResult normalizeResult = null;
            ResolvedTimestamp resolvedTimestamp = null;
            if (decodedPayload.dataFormatType() == MqttDataFormatType.STANDARD_TYPE_3) {
                resolvedTimestamp = resolveTimestamp(map, resolvedDeviceCode);
                message.setTimestamp(resolvedTimestamp.timestamp());
            } else {
                normalizeResult = legacyDpPropertyNormalizer.normalize(map, resolvedDeviceCode);
                message.setTimestamp(normalizeResult.getTimestamp());
            }
            message.setMessageType(resolveMessageType(
                    context,
                    payloadMessageType,
                    map,
                    normalizeResult == null ? null : normalizeResult.getMessageType(),
                    decodedPayload.dataFormatType()
            ));

            boolean childSplitApplied = false;
            if (decodedPayload.dataFormatType() == MqttDataFormatType.STANDARD_TYPE_3) {
                // 表 C.3 / C.4 属于文件或升级分包类消息，当前先收口到事件元数据，
                // 不进入一期最新属性表，避免把文件描述字段误写成设备属性。
                message.setFilePayload(decodedPayload.filePayload());
                message.setEvents(buildFileEvents(map));
            } else {
                normalizeResult = legacyDpChildMessageSplitter.split(map, message, normalizeResult);
                Map<String, Object> properties = normalizeResult.getProperties();
                if (normalizeResult.getChildMessages() != null && !normalizeResult.getChildMessages().isEmpty()) {
                    childSplitApplied = Boolean.TRUE.equals(normalizeResult.getChildSplitApplied());
                    message.setChildMessages(normalizeResult.getChildMessages());
                }
                if (!properties.isEmpty()) {
                    message.setProperties(properties);
                }
            }

            Object events = map.get("events");
            if (events instanceof Map<?, ?>) {
                message.setEvents((Map<String, Object>) events);
            }

            message.setRawPayload(decodedPayload.rawPayload());
            DeviceUpProtocolMetadata protocolMetadata = buildProtocolMetadata(
                    context,
                    decodedPayload.appId(),
                    normalizeResult == null
                            ? legacyDpFamilyResolver.detectFamilyCodes(map, resolvedDeviceCode)
                            : normalizeResult.getFamilyCodes(),
                    normalizeResult == null
                            ? resolvedTimestamp.timestampSource()
                            : normalizeResult.getTimestampSource(),
                    childSplitApplied
            );
            message.setProtocolMetadata(protocolMetadata);
            propagateProtocolMetadataToChildren(message, protocolMetadata);
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

    private String resolveMessageType(ProtocolContext context,
                                      String payloadMessageType,
                                      Map<String, Object> payload,
                                      String normalizedMessageType,
                                      MqttDataFormatType dataFormatType) {
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

    private ResolvedTimestamp resolveTimestamp(Map<String, Object> payload, String resolvedDeviceCode) {
        List<LocalDateTime> timestamps = new ArrayList<>();
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        collectTimestamps(body, timestamps);
        if (!timestamps.isEmpty()) {
            timestamps.sort(LocalDateTime::compareTo);
            return new ResolvedTimestamp(timestamps.get(timestamps.size() - 1), "PAYLOAD_TIMESTAMP");
        }
        return new ResolvedTimestamp(LocalDateTime.now(), "SERVER_TIME");
    }

    private void collectTimestamps(Object source, List<LocalDateTime> timestamps) {
        if (!(source instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && legacyDpFamilyResolver.isTimestampKey(key)) {
                LocalDateTime parsed = legacyDpFamilyResolver.parseTimestamp(key);
                if (parsed != null) {
                    timestamps.add(parsed);
                }
            }
            if (entry.getKey() instanceof String key
                    && ("at".equalsIgnoreCase(key) || "timestamp".equalsIgnoreCase(key) || "ts".equalsIgnoreCase(key))
                    && entry.getValue() != null) {
                LocalDateTime parsed = legacyDpFamilyResolver.parseTimestamp(String.valueOf(entry.getValue()));
                if (parsed != null) {
                    timestamps.add(parsed);
                }
            }
            collectTimestamps(entry.getValue(), timestamps);
        }
    }

    private DeviceUpProtocolMetadata buildProtocolMetadata(ProtocolContext context,
                                                           String appId,
                                                           List<String> familyCodes,
                                                           String timestampSource,
                                                           boolean childSplitApplied) {
        DeviceUpProtocolMetadata metadata = new DeviceUpProtocolMetadata();
        metadata.setAppId(appId);
        metadata.setRouteType(resolveRouteType(context));
        if (isLegacyDpContext(context) && isLegacyDpFamilyObservabilityEnabled()) {
            metadata.setFamilyCodes(familyCodes == null ? List.of() : familyCodes);
            metadata.setTimestampSource(timestampSource);
            metadata.setChildSplitApplied(childSplitApplied);
            metadata.setNormalizationStrategy(isLegacyDpNormalizerV2Enabled() ? "LEGACY_DP" : "LEGACY_DP_COMPAT");
        }
        return metadata;
    }

    private String resolveRouteType(ProtocolContext context) {
        if (context == null) {
            return null;
        }
        String routeType = stringValue(context.getTopicRouteType());
        if (routeType != null && !routeType.isBlank()) {
            return routeType;
        }
        if ("$dp".equals(context.getTopic())) {
            return "legacy";
        }
        return null;
    }

    private boolean isLegacyDpContext(ProtocolContext context) {
        if (context == null) {
            return false;
        }
        return "$dp".equals(context.getTopic())
                || "legacy".equalsIgnoreCase(stringValue(context.getTopicRouteType()));
    }

    private boolean isLegacyDpFamilyObservabilityEnabled() {
        return iotProperties.getProtocol() == null
                || iotProperties.getProtocol().getLegacyDp() == null
                || iotProperties.getProtocol().getLegacyDp().getFamilyObservabilityEnabled() == null
                || Boolean.TRUE.equals(iotProperties.getProtocol().getLegacyDp().getFamilyObservabilityEnabled());
    }

    private boolean isLegacyDpNormalizerV2Enabled() {
        return iotProperties.getProtocol() == null
                || iotProperties.getProtocol().getLegacyDp() == null
                || iotProperties.getProtocol().getLegacyDp().getNormalizerV2Enabled() == null
                || Boolean.TRUE.equals(iotProperties.getProtocol().getLegacyDp().getNormalizerV2Enabled());
    }

    private void propagateProtocolMetadataToChildren(DeviceUpMessage message,
                                                     DeviceUpProtocolMetadata protocolMetadata) {
        if (message == null || protocolMetadata == null || message.getChildMessages() == null) {
            return;
        }
        for (DeviceUpMessage childMessage : message.getChildMessages()) {
            if (childMessage != null && childMessage.getProtocolMetadata() == null) {
                childMessage.setProtocolMetadata(protocolMetadata);
            }
        }
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

    private record ResolvedTimestamp(LocalDateTime timestamp,
                                     String timestampSource) {
    }
}
