package com.ghlzm.iot.protocol.mqtt.legacy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class LegacyDpFamilyResolver {

    private static final List<String> RESERVED_PROPERTY_KEYS = List.of(
            "messageType", "productKey", "product_code", "productCode", "product_key", "pk",
            "deviceCode", "device_code", "deviceId", "device_id", "devId", "dev_id", "imei", "sn",
            "topic", "clientId", "client_id", "timestamp", "ts", "header", "headers", "body", "bodies",
            "_dataFormatType", "_fileStreamLength", "_fileStreamBase64", "_firmwarePacket", "_binaryLength"
    );
    private static final List<String> LEGACY_STATUS_FIELD_ALIASES = List.of(
            "ext_power_volt", "solar_volt", "battery_dump_energy", "signal_4g", "sensor_state", "lon", "lat"
    );

    public List<String> resolveFamilyCodes(Map<String, Object> payload, String resolvedDeviceCode) {
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

    public String inferMessageType(Map<String, Object> payload,
                                   String resolvedDeviceCode,
                                   List<String> familyCodes) {
        if (familyCodes != null) {
            for (String familyCode : familyCodes) {
                if (familyCode != null && familyCode.contains("_ZT_")) {
                    return "status";
                }
            }
        }

        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        if (!(body instanceof Map<?, ?> bodyMap)) {
            return null;
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
}
