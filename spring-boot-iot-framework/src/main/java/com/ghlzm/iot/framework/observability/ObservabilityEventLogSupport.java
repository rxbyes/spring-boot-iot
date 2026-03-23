package com.ghlzm.iot.framework.observability;

import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.StringJoiner;

/**
 * 统一事件摘要日志格式，避免业务链路各写一套文案。
 */
public final class ObservabilityEventLogSupport {

    private ObservabilityEventLogSupport() {
    }

    public static String summary(String event, String result, Long costMs, Map<String, ?> details) {
        StringJoiner joiner = new StringJoiner(" ");
        append(joiner, "event", event);
        append(joiner, "result", result);
        if (costMs != null && costMs >= 0) {
            append(joiner, "costMs", costMs);
        }
        if (details != null && !details.isEmpty()) {
            details.forEach((key, value) -> append(joiner, key, value));
        }
        return joiner.toString();
    }

    private static void append(StringJoiner joiner, String key, Object value) {
        if (!StringUtils.hasText(key) || value == null) {
            return;
        }
        String normalized = normalizeValue(value);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        if (isPlainNumeric(normalized)) {
            joiner.add(key + "=" + normalized);
            return;
        }
        joiner.add(key + "=\"" + escape(normalized) + "\"");
    }

    private static String normalizeValue(Object value) {
        String text = SensitiveLogSanitizer.sanitize(String.valueOf(value));
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }

    private static boolean isPlainNumeric(String text) {
        return text.matches("-?\\d+(\\.\\d+)?");
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
