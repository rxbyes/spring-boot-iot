package com.ghlzm.iot.protocol.mqtt.legacy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * `$dp` 地灾家族识别器。
 */
public class LegacyDpFamilyResolver {

    private static final List<String> RESERVED_PROPERTY_KEYS = List.of(
            "messageType", "productKey", "product_code", "productCode", "product_key", "pk",
            "deviceCode", "device_code", "deviceId", "device_id", "devId", "dev_id", "imei", "sn",
            "topic", "clientId", "client_id", "timestamp", "ts", "header", "headers", "body", "bodies",
            "_dataFormatType", "_fileStreamLength", "_fileStreamBase64", "_firmwarePacket", "_binaryLength"
    );
    private static final Pattern FAMILY_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]+(?:_[A-Za-z0-9]+){2,}$");

    public List<String> detectFamilyCodes(Map<String, Object> payload, String resolvedDeviceCode) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Set<String> familyCodes = new LinkedHashSet<>();
        collectFamilyCodes(payload, familyCodes);
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        collectFamilyCodes(body, familyCodes);
        return new ArrayList<>(familyCodes);
    }

    public boolean isFamilyCode(String key) {
        return key != null
                && !key.isBlank()
                && !RESERVED_PROPERTY_KEYS.contains(key)
                && !isTimestampKey(key)
                && FAMILY_CODE_PATTERN.matcher(key).matches();
    }

    public boolean isTimestampKey(String key) {
        return parseTimestamp(key) != null;
    }

    public LocalDateTime parseTimestamp(String value) {
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

    private void collectFamilyCodes(Object source, Set<String> familyCodes) {
        if (!(source instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && isFamilyCode(key)) {
                familyCodes.add(key);
            }
        }
    }
}
