package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 站内消息投放对象解析工具。
 */
final class InAppMessageDeliverySupport {

    private InAppMessageDeliverySupport() {
    }

    static List<Long> resolveTargetUserIds(InAppMessage message,
                                           Map<Long, User> activeUserMap,
                                           Map<Long, List<RoleSummaryVO>> userRoles) {
        if (message == null || activeUserMap == null || activeUserMap.isEmpty()) {
            return List.of();
        }
        return switch (message.getTargetType()) {
            case "all" -> activeUserMap.keySet().stream().toList();
            case "role" -> {
                Set<String> targetRoles = SystemContentAccessSupport.toUpperCaseSet(
                        SystemContentAccessSupport.splitCsv(message.getTargetRoleCodes())
                );
                yield activeUserMap.keySet().stream()
                        .filter(userId -> userRoles.getOrDefault(userId, List.of()).stream()
                                .map(RoleSummaryVO::getRoleCode)
                                .filter(StringUtils::hasText)
                                .map(value -> value.toUpperCase(Locale.ROOT))
                                .anyMatch(targetRoles::contains))
                        .toList();
            }
            case "user" -> SystemContentAccessSupport.splitCsv(message.getTargetUserIds()).stream()
                    .map(value -> {
                        try {
                            return Long.parseLong(value);
                        } catch (NumberFormatException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(activeUserMap::containsKey)
                    .distinct()
                    .toList();
            default -> List.of();
        };
    }

    static String buildReadKey(Long messageId, Long userId) {
        return messageId + ":" + userId;
    }
}
