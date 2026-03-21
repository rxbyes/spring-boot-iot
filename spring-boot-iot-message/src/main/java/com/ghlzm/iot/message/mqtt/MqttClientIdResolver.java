package com.ghlzm.iot.message.mqtt;

/**
 * MQTT clientId 解析器。
 * 默认共享 clientId 会自动附加主机/PID 片段，降低共享 Broker 上的会话互踢风险。
 */
final class MqttClientIdResolver {

    static final String DEFAULT_SHARED_CLIENT_ID = "spring-boot-iot-mqtt-consumer";
    private static final int MAX_CLIENT_ID_LENGTH = 64;

    private MqttClientIdResolver() {
    }

    static String resolve(String configuredClientId) {
        String baseClientId = hasText(configuredClientId) ? configuredClientId.trim() : DEFAULT_SHARED_CLIENT_ID;
        if (!DEFAULT_SHARED_CLIENT_ID.equals(baseClientId)) {
            return truncate(baseClientId, MAX_CLIENT_ID_LENGTH);
        }
        String runtimeSuffix = sanitize(resolveHost()) + "-" + ProcessHandle.current().pid();
        int maxBaseLength = Math.max(1, MAX_CLIENT_ID_LENGTH - runtimeSuffix.length() - 1);
        return truncate(baseClientId, maxBaseLength) + "-" + runtimeSuffix;
    }

    private static String resolveHost() {
        if (hasText(System.getenv("HOSTNAME"))) {
            return System.getenv("HOSTNAME");
        }
        if (hasText(System.getenv("COMPUTERNAME"))) {
            return System.getenv("COMPUTERNAME");
        }
        return "host";
    }

    private static String sanitize(String rawValue) {
        String value = hasText(rawValue) ? rawValue.trim().toLowerCase() : "host";
        value = value.replaceAll("[^a-z0-9]+", "-");
        value = value.replaceAll("-{2,}", "-");
        value = value.replaceAll("^-|-$", "");
        return hasText(value) ? value : "host";
    }

    private static String truncate(String value, int maxLength) {
        if (!hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
