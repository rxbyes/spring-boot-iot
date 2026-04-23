package com.ghlzm.iot.common.device;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 设备能力类型解析与绑定辅助方法。
 */
public final class DeviceBindingCapabilitySupport {

    public static final String EXTENSION_STATUS_AI_EVENT_RESERVED = "AI_EVENT_RESERVED";

    private static final List<String> VIDEO_KEYWORDS = List.of("video", "camera", "ipc", "视频", "摄像");
    private static final List<String> VIDEO_DEVICE_HINT_KEYWORDS = List.of("video", "camera", "ipc", "视频", "摄像", "监控");
    private static final List<String> WARNING_KEYWORDS = List.of("warning", "warn", "预警", "声光", "爆闪", "广播", "情报板", "报警");
    private static final List<String> MONITORING_KEYWORDS = List.of("monitor", "monitoring", "监测", "gnss", "位移", "倾角", "裂缝", "雨量", "水位", "激光");

    private DeviceBindingCapabilitySupport() {
    }

    public static DeviceBindingCapabilityType normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return DeviceBindingCapabilityType.UNKNOWN;
        }
        try {
            return DeviceBindingCapabilityType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DeviceBindingCapabilityType.UNKNOWN;
        }
    }

    public static DeviceBindingCapabilityType resolve(String productKey, String productName) {
        String normalized = normalizeKeywords(productKey, productName);
        if (!StringUtils.hasText(normalized)) {
            return DeviceBindingCapabilityType.UNKNOWN;
        }
        if (containsAny(normalized, VIDEO_KEYWORDS)) {
            return DeviceBindingCapabilityType.VIDEO;
        }
        if (containsAny(normalized, WARNING_KEYWORDS)) {
            return DeviceBindingCapabilityType.WARNING;
        }
        if (containsAny(normalized, MONITORING_KEYWORDS)) {
            return DeviceBindingCapabilityType.MONITORING;
        }
        return DeviceBindingCapabilityType.UNKNOWN;
    }

    public static DeviceBindingCapabilityType resolve(String productKey,
                                                      String productName,
                                                      String deviceCode,
                                                      String deviceName) {
        DeviceBindingCapabilityType resolved = resolve(productKey, productName);
        if (resolved != DeviceBindingCapabilityType.UNKNOWN) {
            return resolved;
        }
        String normalizedDeviceHints = normalizeKeywords(deviceCode, deviceName);
        if (!StringUtils.hasText(normalizedDeviceHints)) {
            return DeviceBindingCapabilityType.UNKNOWN;
        }
        if (containsAny(normalizedDeviceHints, VIDEO_DEVICE_HINT_KEYWORDS)) {
            return DeviceBindingCapabilityType.VIDEO;
        }
        if (containsAny(normalizedDeviceHints, WARNING_KEYWORDS)) {
            return DeviceBindingCapabilityType.WARNING;
        }
        if (containsAny(normalizedDeviceHints, MONITORING_KEYWORDS)) {
            return DeviceBindingCapabilityType.MONITORING;
        }
        return DeviceBindingCapabilityType.UNKNOWN;
    }

    public static DeviceBindingCapabilityType resolve(String productKey,
                                                      String productName,
                                                      boolean hasFormalMetrics) {
        DeviceBindingCapabilityType resolved = resolve(productKey, productName);
        if (resolved != DeviceBindingCapabilityType.UNKNOWN) {
            return resolved;
        }
        return hasFormalMetrics ? DeviceBindingCapabilityType.MONITORING : DeviceBindingCapabilityType.UNKNOWN;
    }

    public static DeviceBindingCapabilityType resolve(String productKey,
                                                      String productName,
                                                      String deviceCode,
                                                      String deviceName,
                                                      boolean hasFormalMetrics) {
        DeviceBindingCapabilityType resolved = resolve(productKey, productName, deviceCode, deviceName);
        if (resolved != DeviceBindingCapabilityType.UNKNOWN) {
            return resolved;
        }
        return hasFormalMetrics ? DeviceBindingCapabilityType.MONITORING : DeviceBindingCapabilityType.UNKNOWN;
    }

    public static boolean supportsMetricBinding(DeviceBindingCapabilityType type, boolean hasFormalMetrics) {
        DeviceBindingCapabilityType normalizedType = type == null ? DeviceBindingCapabilityType.UNKNOWN : type;
        if (normalizedType == DeviceBindingCapabilityType.UNKNOWN) {
            return hasFormalMetrics;
        }
        return normalizedType.supportsMetricBinding();
    }

    public static boolean isAiEventExpandable(DeviceBindingCapabilityType type) {
        return type == DeviceBindingCapabilityType.VIDEO;
    }

    public static String resolveExtensionStatus(DeviceBindingCapabilityType type) {
        return isAiEventExpandable(type) ? EXTENSION_STATUS_AI_EVENT_RESERVED : null;
    }

    private static String normalizeKeywords(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder merged = new StringBuilder();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (merged.length() > 0) {
                merged.append(' ');
            }
            merged.append(value.trim());
        }
        return merged.toString().toLowerCase(Locale.ROOT);
    }

    private static String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private static boolean containsAny(String source, List<String> keywords) {
        if (!StringUtils.hasText(source) || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && source.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
