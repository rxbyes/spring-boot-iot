package com.ghlzm.iot.framework.observability;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 普通日志与审计日志共用的敏感信息脱敏器。
 */
public final class SensitiveLogSanitizer {

    private static final String SENSITIVE_KEYS =
            "password|token|secret|authorization|accessToken|refreshToken|clientSecret|apiKey|accessKey|privateKey|deviceSecret|merchantKey|signatureSecret";

    private static final Pattern JSON_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\"(" + SENSITIVE_KEYS + ")\"\\s*:\\s*\"[^\"]*\"");
    private static final Pattern ESCAPED_JSON_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\\\\\"(" + SENSITIVE_KEYS + ")\\\\\"\\s*:\\s*\\\\\"[^\\\\\"]*\\\\\"");
    private static final Pattern ESCAPED_JSON_SENSITIVE_GROUP_PATTERN = Pattern.compile(
            "(?i)(\\\\\"(?:" + SENSITIVE_KEYS + ")\\\\\"\\s*:\\s*\\\\\")[^\\\\\"]*(\\\\\")");
    private static final Pattern KV_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(" + SENSITIVE_KEYS + ")=([^&\\s]+)");
    private static final Pattern AUTHORIZATION_HEADER_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*:\\s*bearer\\s+)([^\\s,;]+)");
    private static final Pattern SENSITIVE_KEY_NAME_PATTERN = Pattern.compile("(?i)^(" + SENSITIVE_KEYS + ")$");

    private SensitiveLogSanitizer() {
    }

    public static String sanitize(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String masked = replaceWithPattern(text, JSON_SENSITIVE_PATTERN, "\"$1\":\"***\"");
        masked = replaceWithPattern(masked, ESCAPED_JSON_SENSITIVE_PATTERN, "\\\\\"$1\\\\\":\\\\\"***\\\\\"");
        masked = replaceWithPattern(masked, ESCAPED_JSON_SENSITIVE_GROUP_PATTERN, "$1***$2");
        masked = replaceWithPattern(masked, KV_SENSITIVE_PATTERN, "$1=***");
        masked = replaceWithPattern(masked, AUTHORIZATION_HEADER_PATTERN, "$1***");
        return masked;
    }

    public static boolean isSensitiveKey(String key) {
        return StringUtils.hasText(key) && SENSITIVE_KEY_NAME_PATTERN.matcher(key.trim()).matches();
    }

    private static String replaceWithPattern(String text, Pattern pattern, String replacement) {
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll(replacement);
    }
}
