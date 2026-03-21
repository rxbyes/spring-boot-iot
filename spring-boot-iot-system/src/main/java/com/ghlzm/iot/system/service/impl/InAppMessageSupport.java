package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.InAppMessage;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 站内消息共享规则工具。
 */
final class InAppMessageSupport {

    static final Long DEFAULT_TENANT_ID = 1L;
    static final Long DEFAULT_OPERATOR_ID = 1L;
    static final Set<String> ALLOWED_MESSAGE_TYPES = Set.of("system", "business", "error");
    static final Set<String> ALLOWED_PRIORITIES = Set.of("critical", "high", "medium", "low");
    static final Set<String> ALLOWED_TARGET_TYPES = Set.of("all", "role", "user");
    static final Set<String> AUTOMATIC_SOURCE_TYPES = Set.of("system_error", "event_dispatch", "work_order");
    static final Set<String> CURATED_SOURCE_TYPES = Set.of("manual", "system_error", "event_dispatch", "work_order", "governance");

    private InAppMessageSupport() {
    }

    static String normalizeEnum(String raw,
                                Set<String> allowedValues,
                                String fieldName,
                                String defaultValue) {
        String normalized = StringUtils.hasText(raw) ? raw.trim().toLowerCase(Locale.ROOT) : defaultValue;
        if (!StringUtils.hasText(normalized) || !allowedValues.contains(normalized)) {
            throw new BizException(fieldName + "不合法");
        }
        return normalized;
    }

    static String normalizeSourceType(String raw, String defaultValue) {
        String normalized = StringUtils.hasText(raw) ? raw.trim().toLowerCase(Locale.ROOT) : defaultValue;
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return switch (normalized) {
            case "system_maintenance", "daily_report" -> "manual";
            case "governance_task" -> "governance";
            default -> normalized;
        };
    }

    static String requireText(String raw, String fieldName) {
        String normalized = nullableText(raw);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(fieldName + "不能为空");
        }
        return normalized;
    }

    static String nullableText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }

    static String normalizeUserIdsCsv(String raw) {
        List<String> userIds = SystemContentAccessSupport.splitCsv(raw).stream()
                .map(value -> {
                    try {
                        return String.valueOf(Long.parseLong(value));
                    } catch (NumberFormatException ex) {
                        throw new BizException("目标用户格式不合法");
                    }
                })
                .distinct()
                .toList();
        return String.join(",", userIds);
    }

    static String buildDedupKey(InAppMessage message) {
        if (message == null
                || !StringUtils.hasText(message.getSourceType())
                || !StringUtils.hasText(message.getSourceId())
                || !StringUtils.hasText(message.getMessageType())
                || !StringUtils.hasText(message.getTargetType())) {
            return null;
        }
        String targetFingerprint = switch (message.getTargetType()) {
            case "all" -> "all";
            case "role" -> "role:" + SystemContentAccessSupport.normalizeUpperCaseCsv(message.getTargetRoleCodes());
            case "user" -> "user:" + normalizeUserIdsCsv(message.getTargetUserIds());
            default -> null;
        };
        if (!StringUtils.hasText(targetFingerprint)) {
            return null;
        }
        String raw = String.join("|",
                message.getSourceType(),
                message.getSourceId(),
                targetFingerprint,
                message.getMessageType());
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    static boolean isAutomaticSourceType(String sourceType) {
        return StringUtils.hasText(sourceType) && AUTOMATIC_SOURCE_TYPES.contains(sourceType.trim().toLowerCase(Locale.ROOT));
    }

    static Date resolveDefaultExpireTime(String sourceType, Date publishTime, Date expireTime) {
        if (expireTime != null || publishTime == null) {
            return expireTime;
        }
        long publishTimeMs = publishTime.getTime();
        String normalizedSourceType = normalizeSourceType(sourceType, null);
        if ("system_error".equals(normalizedSourceType)) {
            return new Date(publishTimeMs + 24L * 60L * 60L * 1000L);
        }
        if ("event_dispatch".equals(normalizedSourceType) || "work_order".equals(normalizedSourceType)) {
            return new Date(publishTimeMs + 7L * 24L * 60L * 60L * 1000L);
        }
        return null;
    }
}
