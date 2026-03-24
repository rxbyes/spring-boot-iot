package com.ghlzm.iot.framework.observability;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 普通日志与审计日志共用的敏感信息脱敏器。
 */
public final class SensitiveLogSanitizer {

    private static final Pattern JSON_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\"(password|token|secret|authorization|accessToken|refreshToken|clientSecret)\"\\s*:\\s*\"[^\"]*\"");
    private static final Pattern ESCAPED_JSON_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\\\\\"(password|token|secret|authorization|accessToken|refreshToken|clientSecret)\\\\\"\\s*:\\s*\\\\\"[^\\\\\"]*\\\\\"");
    private static final Pattern ESCAPED_JSON_SENSITIVE_GROUP_PATTERN = Pattern.compile(
            "(?i)(\\\\\"(?:password|token|secret|authorization|accessToken|refreshToken|clientSecret)\\\\\"\\s*:\\s*\\\\\")[^\\\\\"]*(\\\\\")");
    private static final Pattern KV_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|token|secret|authorization|accessToken|refreshToken|clientSecret)=([^&\\s]+)");
    private static final Pattern AUTHORIZATION_HEADER_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*:\\s*bearer\\s+)([^\\s,;]+)");

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

    private static String replaceWithPattern(String text, Pattern pattern, String replacement) {
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll(replacement);
    }
}
