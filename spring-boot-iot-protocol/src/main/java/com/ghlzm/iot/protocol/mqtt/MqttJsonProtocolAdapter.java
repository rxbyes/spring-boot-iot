package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.model.DeviceFilePayload;
import com.ghlzm.iot.protocol.core.model.DeviceFirmwarePacket;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
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

    public MqttJsonProtocolAdapter(MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry,
                                   MqttPayloadFrameParser mqttPayloadFrameParser,
                                   MqttPayloadSecurityValidator mqttPayloadSecurityValidator,
                                   MqttFirmwarePacketParser mqttFirmwarePacketParser,
                                   IotProperties iotProperties) {
        this.mqttPayloadDecryptorRegistry = mqttPayloadDecryptorRegistry;
        this.mqttPayloadFrameParser = mqttPayloadFrameParser;
        this.mqttPayloadSecurityValidator = mqttPayloadSecurityValidator;
        this.mqttFirmwarePacketParser = mqttFirmwarePacketParser;
        this.iotProperties = iotProperties;
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
            message.setMessageType(resolveMessageType(context, payloadMessageType, map, resolvedDeviceCode, decodedPayload.dataFormatType()));
            message.setTopic(context.getTopic());
            message.setDataFormatType(decodedPayload.dataFormatType() == null
                    ? null
                    : decodedPayload.dataFormatType().name());
            LocalDateTime resolvedTimestamp = resolveTimestamp(map, resolvedDeviceCode);
            message.setTimestamp(resolvedTimestamp);

            if (decodedPayload.dataFormatType() == MqttDataFormatType.STANDARD_TYPE_3) {
                // 表 C.3 / C.4 属于文件或升级分包类消息，当前先收口到事件元数据，
                // 不进入一期最新属性表，避免把文件描述字段误写成设备属性。
                message.setFilePayload(decodedPayload.filePayload());
                message.setEvents(buildFileEvents(map));
            } else {
                Map<String, Object> properties = resolveProperties(map, resolvedDeviceCode);
                ConfiguredChildMessages configuredChildMessages = buildConfiguredChildMessages(
                        map,
                        resolvedDeviceCode,
                        message.getTenantId(),
                        message.getProductKey(),
                        message.getMessageType(),
                        message.getTopic(),
                        resolvedTimestamp
                );
                if (!configuredChildMessages.messages().isEmpty()) {
                    properties = removeChildLogicalProperties(properties, configuredChildMessages.logicalCodes());
                    message.setChildMessages(configuredChildMessages.messages());
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
                                      String resolvedDeviceCode,
                                      MqttDataFormatType dataFormatType) {
        if (dataFormatType == MqttDataFormatType.STANDARD_TYPE_3) {
            return inferFileMessageType(payload);
        }
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

    private DecodedPayload decodePayload(byte[] payload) throws Exception {
        MqttPayloadFrameParser.ParsedFrame parsedFrame = mqttPayloadFrameParser.parse("mqtt-json", payload);
        String payloadText = sanitizePayload(parsedFrame.jsonMessage());
        Map<String, Object> payloadMap = objectMapper.readValue(payloadText, new TypeReference<>() {
        });

        if (isEncryptedEnvelope(payloadMap)) {
            String appId = extractAppId(payloadMap);
            String encryptedBody = extractEncryptedBody(payloadMap);
            mqttPayloadSecurityValidator.validateEnvelope(appId, payloadMap, encryptedBody);
            // 密文解开后，真实设备可能返回“类型字节 + 长度字节 + JSON”的二进制内容，
            // 因此这里要再次经过帧解析，不能直接把明文当作纯 JSON 处理。
            MqttPayloadFrameParser.ParsedFrame decryptedFrame = mqttPayloadFrameParser.parse(
                    "mqtt-json-decrypted",
                    mqttPayloadDecryptorRegistry.decryptBytesOrThrow(appId, encryptedBody)
            );
            String plaintext = sanitizePayload(decryptedFrame.jsonMessage());
            Map<String, Object> decryptedMap = objectMapper.readValue(plaintext, new TypeReference<>() {
            });
            enrichByDataFormat(decryptedMap, decryptedFrame);
            // 原始日志仍保留接入时的密文报文，便于审计与排障。
            return new DecodedPayload(
                    decryptedMap,
                    payloadText,
                    decryptedFrame.dataFormatType(),
                    buildFilePayload(decryptedMap, decryptedFrame)
            );
        }

        enrichByDataFormat(payloadMap, parsedFrame);
        return new DecodedPayload(
                payloadMap,
                buildRawPayloadForLog(parsedFrame, payloadText, payloadMap),
                parsedFrame.dataFormatType(),
                buildFilePayload(payloadMap, parsedFrame)
        );
    }

    private void enrichByDataFormat(Map<String, Object> payloadMap, MqttPayloadFrameParser.ParsedFrame parsedFrame) {
        if (payloadMap == null || parsedFrame == null) {
            return;
        }
        payloadMap.put("_dataFormatType", parsedFrame.dataFormatType().name());
        if (parsedFrame.dataFormatType() == MqttDataFormatType.STANDARD_TYPE_3 && parsedFrame.binaryPayload() != null) {
            payloadMap.put("_binaryLength", parsedFrame.binaryLength());
            payloadMap.put("_fileStreamLength", parsedFrame.binaryLength());
            payloadMap.put("_fileStreamBase64", Base64.getEncoder().encodeToString(parsedFrame.binaryPayload()));
            if (shouldParseFirmwarePacket(payloadMap)) {
                MqttFirmwarePacketParser.ParsedFirmwarePacket packet =
                        mqttFirmwarePacketParser.parse("mqtt-json-firmware", parsedFrame.binaryPayload());
                Map<String, Object> firmwarePacket = new LinkedHashMap<>();
                firmwarePacket.put("packetIndex", packet.packetIndex());
                firmwarePacket.put("packetSize", packet.packetSize());
                firmwarePacket.put("totalPackets", packet.totalPackets());
                firmwarePacket.put("packetDataBase64", Base64.getEncoder().encodeToString(packet.packetData()));
                firmwarePacket.put("md5Length", packet.md5Length());
                firmwarePacket.put("firmwareMd5", packet.firmwareMd5());
                payloadMap.put("_firmwarePacket", firmwarePacket);
            }
        }
    }

    private boolean shouldParseFirmwarePacket(Map<String, Object> payloadMap) {
        String fileType = stringValue(payloadMap.get("file_type"));
        if (fileType != null) {
            String normalized = fileType.trim().toLowerCase();
            if ("bin".equals(normalized) || "firmware".equals(normalized) || "ota".equals(normalized)) {
                return true;
            }
        }
        String dsId = stringValue(payloadMap.get("ds_id"));
        return dsId != null && (dsId.toLowerCase().contains("firmware") || dsId.toLowerCase().contains("ota"));
    }

    private String buildRawPayloadForLog(MqttPayloadFrameParser.ParsedFrame parsedFrame,
                                         String payloadText,
                                         Map<String, Object> payloadMap) throws Exception {
        if (parsedFrame != null
                && parsedFrame.dataFormatType() == MqttDataFormatType.STANDARD_TYPE_3
                && parsedFrame.binaryPayload() != null) {
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put("dataFormatType", parsedFrame.dataFormatType().name());
            rawPayload.put("descriptor", payloadMap);
            rawPayload.put("binaryLength", parsedFrame.binaryLength());
            rawPayload.put("binaryBase64", Base64.getEncoder().encodeToString(parsedFrame.binaryPayload()));
            return objectMapper.writeValueAsString(rawPayload);
        }
        return payloadText;
    }

    private DeviceFilePayload buildFilePayload(Map<String, Object> payloadMap,
                                               MqttPayloadFrameParser.ParsedFrame parsedFrame) {
        if (payloadMap == null || parsedFrame == null || parsedFrame.dataFormatType() != MqttDataFormatType.STANDARD_TYPE_3) {
            return null;
        }

        DeviceFilePayload filePayload = new DeviceFilePayload();
        filePayload.setDeviceId(stringValue(payloadMap.get("did")));
        filePayload.setDataSetId(stringValue(payloadMap.get("ds_id")));
        filePayload.setFileType(stringValue(payloadMap.get("file_type")));
        filePayload.setDescription(stringValue(payloadMap.get("desc")));
        filePayload.setTimestamp(parseTimestamp(stringValue(payloadMap.get("at"))));
        filePayload.setBinaryLength(parsedFrame.binaryLength());
        filePayload.setBinaryPayload(parsedFrame.safeBinaryPayload());
        filePayload.setDescriptor(new LinkedHashMap<>(payloadMap));

        Object firmwarePacket = payloadMap.get("_firmwarePacket");
        if (firmwarePacket instanceof Map<?, ?> firmwareMap) {
            filePayload.setFirmwarePacket(toFirmwarePacket(firmwareMap));
        }
        return filePayload;
    }

    private DeviceFirmwarePacket toFirmwarePacket(Map<?, ?> firmwareMap) {
        DeviceFirmwarePacket firmwarePacket = new DeviceFirmwarePacket();
        firmwarePacket.setPacketIndex(integerValue(firmwareMap.get("packetIndex")));
        firmwarePacket.setPacketSize(integerValue(firmwareMap.get("packetSize")));
        firmwarePacket.setTotalPackets(integerValue(firmwareMap.get("totalPackets")));
        firmwarePacket.setMd5Length(integerValue(firmwareMap.get("md5Length")));
        firmwarePacket.setFirmwareMd5(stringValue(firmwareMap.get("firmwareMd5")));
        Object packetDataBase64 = firmwareMap.get("packetDataBase64");
        if (packetDataBase64 != null) {
            firmwarePacket.setPacketData(Base64.getDecoder().decode(String.valueOf(packetDataBase64)));
        }
        return firmwarePacket;
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String sanitizePayload(String payloadText) {
        return JsonPayloadUtils.normalizeJsonDocument(payloadText);
    }

    private String buildDecodeFailureMessage(Exception exception) {
        String detail = exception == null ? null : exception.getMessage();
        if (detail == null || detail.isBlank()) {
            return "MQTT JSON 协议解析失败";
        }
        return "MQTT JSON 协议解析失败: " + detail;
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

    private record DecodedPayload(Map<String, Object> payload,
                                  String rawPayload,
                                  MqttDataFormatType dataFormatType,
                                  DeviceFilePayload filePayload) {
    }

    private record LatestLogicalPayload(LocalDateTime timestamp,
                                        Map<String, Object> properties,
                                        String rawPayload) {
    }

    private record TimestampedValue(String key,
                                    LocalDateTime timestamp,
                                    Object value) {
    }

    private record ConfiguredChildMessages(List<DeviceUpMessage> messages,
                                           List<String> logicalCodes) {

        private static ConfiguredChildMessages empty() {
            return new ConfiguredChildMessages(List.of(), List.of());
        }
    }
}
