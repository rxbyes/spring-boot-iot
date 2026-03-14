package com.ghlzm.iot.protocol.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
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
            "deviceCode", "device_code", "deviceId", "device_id", "devId", "dev_id", "imei", "sn"
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
            "topic", "clientId", "client_id", "timestamp", "ts", "header", "headers", "body", "bodies"
    );

    /**
     * 当前阶段协议层只需要最小 JSON 解析能力，直接在适配器内部维护 ObjectMapper，
     * 避免联调测试依赖额外的 Jackson Bean 注入条件。
     */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry;
    private final MqttPayloadFrameParser mqttPayloadFrameParser;

    public MqttJsonProtocolAdapter(MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry,
                                   MqttPayloadFrameParser mqttPayloadFrameParser) {
        this.mqttPayloadDecryptorRegistry = mqttPayloadDecryptorRegistry;
        this.mqttPayloadFrameParser = mqttPayloadFrameParser;
    }

    @Override
    public String getProtocolCode() {
        return "mqtt-json";
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeviceUpMessage decode(byte[] payload, ProtocolContext context) {
        try {
            DecodedPayload decodedPayload = decodePayload(payload);
            Map<String, Object> map = decodedPayload.payload();
            DeviceUpMessage message = new DeviceUpMessage();
            message.setTenantId(context.getTenantCode());
            // 标准 topic 优先，历史兼容 topic 再回退到 payload 字段或设备报文根节点提取。
            String resolvedDeviceCode = resolveDeviceCode(context, map);
            message.setProductKey(resolveIdentity(context.getProductKey(), map, PRODUCT_KEY_ALIASES));
            message.setDeviceCode(resolvedDeviceCode);
            // MQTT 场景优先使用 topic 解析出的 messageType，payload 中的 messageType 作为回退信息。
            String payloadMessageType = stringValue(map.get("messageType"));
            message.setMessageType(resolveMessageType(context, payloadMessageType, map, resolvedDeviceCode));
            message.setTopic(context.getTopic());

            Map<String, Object> properties = resolveProperties(map, resolvedDeviceCode);
            if (!properties.isEmpty()) {
                message.setProperties(properties);
            }

            Object events = map.get("events");
            if (events instanceof Map<?, ?>) {
                message.setEvents((Map<String, Object>) events);
            }

            message.setTimestamp(resolveTimestamp(map, resolvedDeviceCode));
            message.setRawPayload(decodedPayload.rawPayload());
            return message;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception e) {
            throw new BizException("MQTT JSON 协议解析失败");
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
                                      String resolvedDeviceCode) {
        if ("$dp".equals(context.getTopic())) {
            String inferredType = inferLegacyMessageType(payload, resolvedDeviceCode);
            if (inferredType != null && !inferredType.isBlank()) {
                return inferredType;
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
            if (!entries.isEmpty() && entries.get(entries.size() - 1).getValue() instanceof Map<?, ?> latestMap) {
                flattenLegacyProperties(prefix, latestMap, target);
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
            if (!(entry.getKey() instanceof String key) || !isTimestampKey(key) || !(entry.getValue() instanceof Map<?, ?>)) {
                return false;
            }
        }
        return true;
    }

    private LocalDateTime resolveTimestamp(Map<String, Object> payload, String resolvedDeviceCode) {
        List<LocalDateTime> timestamps = new ArrayList<>();
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        collectTimestamps(body, timestamps);
        if (!timestamps.isEmpty()) {
            timestamps.sort(LocalDateTime::compareTo);
            return timestamps.get(timestamps.size() - 1);
        }
        return LocalDateTime.now();
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
            collectTimestamps(entry.getValue(), timestamps);
        }
    }

    private boolean isTimestampKey(String key) {
        return parseTimestamp(key) != null;
    }

    private LocalDateTime parseTimestamp(String value) {
        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
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

    private DecodedPayload decodePayload(byte[] payload) throws Exception {
        MqttPayloadFrameParser.ParsedFrame parsedFrame = mqttPayloadFrameParser.parse("mqtt-json", payload);
        String payloadText = sanitizePayload(parsedFrame.jsonMessage());
        Map<String, Object> payloadMap = objectMapper.readValue(payloadText, new TypeReference<>() {
        });

        if (isEncryptedEnvelope(payloadMap)) {
            String appId = extractAppId(payloadMap);
            String encryptedBody = extractEncryptedBody(payloadMap);
            // 密文解开后，真实设备可能返回“类型字节 + 长度字节 + JSON”的二进制内容，
            // 因此这里要再次经过帧解析，不能直接把明文当作纯 JSON 处理。
            MqttPayloadFrameParser.ParsedFrame decryptedFrame = mqttPayloadFrameParser.parse(
                    "mqtt-json-decrypted",
                    mqttPayloadDecryptorRegistry.decryptBytesOrThrow(appId, encryptedBody)
            );
            String plaintext = sanitizePayload(decryptedFrame.jsonMessage());
            Map<String, Object> decryptedMap = objectMapper.readValue(plaintext, new TypeReference<>() {
            });
            // 原始日志仍保留接入时的密文报文，便于审计与排障。
            return new DecodedPayload(decryptedMap, payloadText);
        }

        return new DecodedPayload(payloadMap, payloadText);
    }

    private String sanitizePayload(String payloadText) {
        if (payloadText == null) {
            return null;
        }
        int startIndex = 0;
        while (startIndex < payloadText.length()) {
            char current = payloadText.charAt(startIndex);
            if (current == '{' || current == '[') {
                break;
            }
            startIndex++;
        }
        return payloadText.substring(startIndex).trim();
    }

    private boolean isEncryptedEnvelope(Map<String, Object> payloadMap) {
        return payloadMap.get("header") instanceof Map<?, ?>
                && payloadMap.get("bodies") instanceof Map<?, ?>;
    }

    private String extractAppId(Map<String, Object> payloadMap) {
        Object header = payloadMap.get("header");
        if (header instanceof Map<?, ?> headerMap) {
            Object appId = headerMap.get("appId");
            if (appId != null) {
                return String.valueOf(appId);
            }
        }
        throw new BizException("加密 MQTT 报文缺少 header.appId");
    }

    private String extractEncryptedBody(Map<String, Object> payloadMap) {
        Object bodies = payloadMap.get("bodies");
        if (bodies instanceof Map<?, ?> bodyMap) {
            Object encryptedBody = bodyMap.get("body");
            if (encryptedBody != null) {
                return String.valueOf(encryptedBody);
            }
        }
        throw new BizException("加密 MQTT 报文缺少 bodies.body");
    }

    private record DecodedPayload(Map<String, Object> payload, String rawPayload) {
    }
}
