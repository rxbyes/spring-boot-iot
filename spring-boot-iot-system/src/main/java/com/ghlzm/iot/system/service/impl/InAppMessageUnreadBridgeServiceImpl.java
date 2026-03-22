package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.entity.InAppMessageBridgeLog;
import com.ghlzm.iot.system.entity.InAppMessageRead;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.InAppMessageBridgeLogMapper;
import com.ghlzm.iot.system.mapper.InAppMessageMapper;
import com.ghlzm.iot.system.mapper.InAppMessageReadMapper;
import com.ghlzm.iot.system.service.InAppMessageUnreadBridgeService;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站内消息高优未读桥接服务。
 */
@Slf4j
@Service
public class InAppMessageUnreadBridgeServiceImpl implements InAppMessageUnreadBridgeService {

    static final String BRIDGE_SCENE = "in_app_unread_bridge";

    private static final String APP_NAME = "spring-boot-iot";
    private static final int RECIPIENT_PREVIEW_LIMIT = 3;
    private static final int MAX_RECIPIENT_SNAPSHOT_LENGTH = 500;
    private static final int MAX_RESPONSE_BODY_LENGTH = 1000;
    private static final int MAX_CONTENT_PREVIEW_LENGTH = 200;
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final InAppMessageMapper inAppMessageMapper;
    private final InAppMessageReadMapper inAppMessageReadMapper;
    private final InAppMessageBridgeLogMapper inAppMessageBridgeLogMapper;
    private final UserService userService;
    private final PermissionService permissionService;
    private final NotificationChannelDispatcher notificationChannelDispatcher;
    private final SystemContentSchemaSupport systemContentSchemaSupport;
    private final IotProperties iotProperties;

    public InAppMessageUnreadBridgeServiceImpl(InAppMessageMapper inAppMessageMapper,
                                               InAppMessageReadMapper inAppMessageReadMapper,
                                               InAppMessageBridgeLogMapper inAppMessageBridgeLogMapper,
                                               UserService userService,
                                               PermissionService permissionService,
                                               NotificationChannelDispatcher notificationChannelDispatcher,
                                               SystemContentSchemaSupport systemContentSchemaSupport,
                                               IotProperties iotProperties) {
        this.inAppMessageMapper = inAppMessageMapper;
        this.inAppMessageReadMapper = inAppMessageReadMapper;
        this.inAppMessageBridgeLogMapper = inAppMessageBridgeLogMapper;
        this.userService = userService;
        this.permissionService = permissionService;
        this.notificationChannelDispatcher = notificationChannelDispatcher;
        this.systemContentSchemaSupport = systemContentSchemaSupport;
        this.iotProperties = iotProperties;
    }

    @Override
    public void scanAndBridgeUnreadMessages() {
        IotProperties.Observability.InAppUnreadBridge config = iotProperties.getObservability().getInAppUnreadBridge();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }

        try {
            systemContentSchemaSupport.ensureInAppMessageReady();
            systemContentSchemaSupport.ensureInAppMessageReadReady();
            systemContentSchemaSupport.ensureInAppMessageBridgeLogReady();
        } catch (BizException ex) {
            log.warn("站内消息未读桥接已跳过，原因：{}", ex.getMessage());
            return;
        }

        List<NotificationChannelDispatcher.DispatchChannel> channels = notificationChannelDispatcher.listSceneChannels(BRIDGE_SCENE);
        if (channels.isEmpty()) {
            return;
        }

        Date now = new Date();
        List<InAppMessage> candidates = listBridgeCandidates(now);
        if (candidates.isEmpty()) {
            return;
        }

        List<User> activeUsers = userService.listUsers(null, null, null, 1);
        if (activeUsers == null || activeUsers.isEmpty()) {
            return;
        }
        Map<Long, User> activeUserMap = activeUsers.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        if (activeUserMap.isEmpty()) {
            return;
        }

        Map<Long, List<RoleSummaryVO>> userRoles = permissionService.listUserRolesByUserIds(activeUserMap.keySet());
        userRoles = userRoles == null ? Map.of() : userRoles;
        Set<String> readKeys = queryReadKeysByMessageIds(candidates.stream().map(InAppMessage::getId).toList());
        Map<String, InAppMessageBridgeLog> bridgeLogMap = queryBridgeLogMap(candidates.stream().map(InAppMessage::getId).toList());

        for (InAppMessage message : candidates) {
            int thresholdMinutes = resolveThresholdMinutes(message.getPriority(), config);
            if (thresholdMinutes <= 0 || !isThresholdReached(message, thresholdMinutes, now)) {
                continue;
            }

            List<Long> unreadUserIds = resolveUnreadUserIds(message, activeUserMap, userRoles, readKeys);
            if (unreadUserIds.isEmpty()) {
                continue;
            }

            String recipientSnapshot = buildRecipientSnapshot(unreadUserIds, activeUserMap);
            NotificationChannelDispatcher.NotificationEnvelope envelope =
                    buildBridgeEnvelope(message, unreadUserIds, activeUserMap, thresholdMinutes);

            for (NotificationChannelDispatcher.DispatchChannel channel : channels) {
                String bridgeLogKey = buildBridgeLogKey(message.getId(), channel.channel().getChannelCode());
                InAppMessageBridgeLog existingLog = bridgeLogMap.get(bridgeLogKey);
                if (shouldSkipAttempt(existingLog, channel.config().minIntervalSeconds(), now)) {
                    continue;
                }

                NotificationChannelDispatcher.DispatchResult result = notificationChannelDispatcher.send(channel, envelope);
                InAppMessageBridgeLog updatedLog = saveBridgeLog(
                        existingLog,
                        message,
                        channel,
                        unreadUserIds.size(),
                        recipientSnapshot,
                        result,
                        now
                );
                bridgeLogMap.put(bridgeLogKey, updatedLog);

                if (result.success()) {
                    log.info("站内消息未读桥接发送成功, messageId={}, channelCode={}, unreadCount={}",
                            message.getId(), channel.channel().getChannelCode(), unreadUserIds.size());
                } else {
                    log.warn("站内消息未读桥接发送失败, messageId={}, channelCode={}, reason={}",
                            message.getId(), channel.channel().getChannelCode(), safeText(result.errorMessage(), result.responseBody()));
                }
            }
        }
    }

    private List<InAppMessage> listBridgeCandidates(Date now) {
        LambdaQueryWrapper<InAppMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessage::getDeleted, 0)
                .eq(InAppMessage::getStatus, 1)
                .in(InAppMessage::getPriority, List.of("high", "critical"))
                .le(InAppMessage::getPublishTime, now)
                .and(wrapper -> wrapper.isNull(InAppMessage::getExpireTime)
                        .or()
                        .gt(InAppMessage::getExpireTime, now))
                .orderByAsc(InAppMessage::getPublishTime)
                .orderByDesc(InAppMessage::getId);
        return inAppMessageMapper.selectList(queryWrapper);
    }

    private Set<String> queryReadKeysByMessageIds(Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<InAppMessageRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(InAppMessageRead::getMessageId, messageIds);
        return inAppMessageReadMapper.selectList(queryWrapper).stream()
                .filter(item -> item.getMessageId() != null && item.getUserId() != null)
                .map(item -> InAppMessageDeliverySupport.buildReadKey(item.getMessageId(), item.getUserId()))
                .collect(Collectors.toSet());
    }

    private Map<String, InAppMessageBridgeLog> queryBridgeLogMap(Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<InAppMessageBridgeLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(InAppMessageBridgeLog::getMessageId, messageIds)
                .eq(InAppMessageBridgeLog::getBridgeScene, BRIDGE_SCENE);
        return inAppMessageBridgeLogMapper.selectList(queryWrapper).stream()
                .filter(item -> item.getMessageId() != null && StringUtils.hasText(item.getChannelCode()))
                .collect(Collectors.toMap(
                        item -> buildBridgeLogKey(item.getMessageId(), item.getChannelCode()),
                        Function.identity(),
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private List<Long> resolveUnreadUserIds(InAppMessage message,
                                            Map<Long, User> activeUserMap,
                                            Map<Long, List<RoleSummaryVO>> userRoles,
                                            Set<String> readKeys) {
        return InAppMessageDeliverySupport.resolveTargetUserIds(message, activeUserMap, userRoles).stream()
                .filter(userId -> !readKeys.contains(InAppMessageDeliverySupport.buildReadKey(message.getId(), userId)))
                .toList();
    }

    private boolean shouldSkipAttempt(InAppMessageBridgeLog bridgeLog, int minIntervalSeconds, Date now) {
        if (bridgeLog == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(bridgeLog.getBridgeStatus())) {
            return true;
        }
        if (minIntervalSeconds <= 0 || bridgeLog.getLastAttemptTime() == null) {
            return false;
        }
        long elapsedMillis = now.getTime() - bridgeLog.getLastAttemptTime().getTime();
        return elapsedMillis < minIntervalSeconds * 1000L;
    }

    private InAppMessageBridgeLog saveBridgeLog(InAppMessageBridgeLog existingLog,
                                                InAppMessage message,
                                                NotificationChannelDispatcher.DispatchChannel channel,
                                                int unreadCount,
                                                String recipientSnapshot,
                                                NotificationChannelDispatcher.DispatchResult result,
                                                Date now) {
        InAppMessageBridgeLog bridgeLog = existingLog == null ? new InAppMessageBridgeLog() : existingLog;
        if (bridgeLog.getId() == null) {
            bridgeLog.setTenantId(message.getTenantId() == null ? InAppMessageSupport.DEFAULT_TENANT_ID : message.getTenantId());
            bridgeLog.setMessageId(message.getId());
            bridgeLog.setChannelCode(channel.channel().getChannelCode());
            bridgeLog.setBridgeScene(BRIDGE_SCENE);
            bridgeLog.setCreateTime(now);
            bridgeLog.setAttemptCount(0);
        }
        bridgeLog.setUnreadCount(unreadCount);
        bridgeLog.setRecipientSnapshot(truncate(recipientSnapshot, MAX_RECIPIENT_SNAPSHOT_LENGTH));
        bridgeLog.setBridgeStatus(result.success() ? 1 : 0);
        bridgeLog.setResponseStatusCode(result.statusCode());
        bridgeLog.setResponseBody(truncate(safeText(result.responseBody(), result.errorMessage()), MAX_RESPONSE_BODY_LENGTH));
        bridgeLog.setLastAttemptTime(now);
        bridgeLog.setSuccessTime(result.success() ? now : bridgeLog.getSuccessTime());
        bridgeLog.setAttemptCount((bridgeLog.getAttemptCount() == null ? 0 : bridgeLog.getAttemptCount()) + 1);
        bridgeLog.setUpdateTime(now);
        if (existingLog == null) {
            inAppMessageBridgeLogMapper.insert(bridgeLog);
        } else {
            inAppMessageBridgeLogMapper.updateById(bridgeLog);
        }
        return bridgeLog;
    }

    private NotificationChannelDispatcher.NotificationEnvelope buildBridgeEnvelope(InAppMessage message,
                                                                                   List<Long> unreadUserIds,
                                                                                   Map<Long, User> activeUserMap,
                                                                                   int thresholdMinutes) {
        String messageTypeLabel = resolveMessageTypeLabel(message.getMessageType());
        String priorityLabel = resolvePriorityLabel(message.getPriority());
        String relatedPath = StringUtils.hasText(message.getRelatedPath()) ? message.getRelatedPath() : "-";
        String publishTime = message.getPublishTime() == null ? "-" : TIME_FORMATTER.format(message.getPublishTime());
        String summary = truncate(
                StringUtils.hasText(message.getSummary()) ? message.getSummary() : safeText(message.getContent(), "-"),
                MAX_CONTENT_PREVIEW_LENGTH
        );
        String recipientSnapshot = buildRecipientSnapshot(unreadUserIds, activeUserMap);

        Map<String, Object> genericPayload = new LinkedHashMap<>();
        genericPayload.put("application", APP_NAME);
        genericPayload.put("eventType", BRIDGE_SCENE);
        genericPayload.put("messageId", message.getId());
        genericPayload.put("messageType", message.getMessageType());
        genericPayload.put("priority", message.getPriority());
        genericPayload.put("title", message.getTitle());
        genericPayload.put("summary", summary);
        genericPayload.put("relatedPath", message.getRelatedPath());
        genericPayload.put("sourceType", message.getSourceType());
        genericPayload.put("sourceId", message.getSourceId());
        genericPayload.put("publishTime", publishTime);
        genericPayload.put("thresholdMinutes", thresholdMinutes);
        genericPayload.put("unreadCount", unreadUserIds.size());
        genericPayload.put("recipientSummary", recipientSnapshot);
        genericPayload.put("unreadUserIds", List.copyOf(unreadUserIds));

        String markdown = """
                ### spring-boot-iot 高优未读桥接
                - 消息ID：%s
                - 标题：%s
                - 分类：%s
                - 优先级：%s
                - 发布时间：%s
                - 阈值：%s 分钟仍未读
                - 未读对象：%s
                - 关联页面：%s
                - 来源：%s / %s
                - 摘要：%s
                - 建议动作：请进入通知中心查看详情，并按关联页面继续处理或显式标记已读。
                """.formatted(
                safeText(message.getId()),
                safeText(message.getTitle()),
                messageTypeLabel,
                priorityLabel,
                publishTime,
                thresholdMinutes,
                recipientSnapshot,
                relatedPath,
                safeText(message.getSourceType(), "-"),
                safeText(message.getSourceId(), "-"),
                summary
        );
        String plainText = """
                spring-boot-iot 高优未读桥接
                消息ID: %s
                标题: %s
                分类: %s
                优先级: %s
                发布时间: %s
                阈值: %s 分钟仍未读
                未读对象: %s
                关联页面: %s
                来源: %s / %s
                摘要: %s
                建议动作: 请进入通知中心查看详情，并按关联页面继续处理或显式标记已读。
                """.formatted(
                safeText(message.getId()),
                safeText(message.getTitle()),
                messageTypeLabel,
                priorityLabel,
                publishTime,
                thresholdMinutes,
                recipientSnapshot,
                relatedPath,
                safeText(message.getSourceType(), "-"),
                safeText(message.getSourceId(), "-"),
                summary
        );
        return new NotificationChannelDispatcher.NotificationEnvelope(
                "spring-boot-iot 高优未读桥接",
                plainText,
                markdown,
                genericPayload
        );
    }

    private boolean isThresholdReached(InAppMessage message, int thresholdMinutes, Date now) {
        if (message == null || message.getPublishTime() == null || thresholdMinutes <= 0) {
            return false;
        }
        long elapsedMillis = now.getTime() - message.getPublishTime().getTime();
        return elapsedMillis >= thresholdMinutes * 60_000L;
    }

    private int resolveThresholdMinutes(String priority, IotProperties.Observability.InAppUnreadBridge config) {
        String normalizedPriority = StringUtils.hasText(priority) ? priority.trim().toLowerCase(Locale.ROOT) : "";
        return switch (normalizedPriority) {
            case "critical" -> config.getCriticalThresholdMinutes() == null ? 10 : config.getCriticalThresholdMinutes();
            case "high" -> config.getHighThresholdMinutes() == null ? 30 : config.getHighThresholdMinutes();
            default -> -1;
        };
    }

    private String buildRecipientSnapshot(List<Long> unreadUserIds, Map<Long, User> activeUserMap) {
        if (unreadUserIds == null || unreadUserIds.isEmpty()) {
            return "无未读对象";
        }
        List<String> displayNames = unreadUserIds.stream()
                .map(activeUserMap::get)
                .map(this::resolveUserDisplayName)
                .filter(StringUtils::hasText)
                .limit(RECIPIENT_PREVIEW_LIMIT)
                .toList();
        String preview = displayNames.isEmpty() ? "未识别用户" : String.join("、", displayNames);
        if (unreadUserIds.size() <= RECIPIENT_PREVIEW_LIMIT) {
            return preview + "（共 " + unreadUserIds.size() + " 人）";
        }
        return preview + " 等 " + unreadUserIds.size() + " 人";
    }

    private String resolveUserDisplayName(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return String.valueOf(user.getId());
    }

    private String resolveMessageTypeLabel(String messageType) {
        return switch (StringUtils.hasText(messageType) ? messageType : "") {
            case "system" -> "系统事件";
            case "business" -> "业务事件";
            case "error" -> "错误事件";
            default -> "站内消息";
        };
    }

    private String resolvePriorityLabel(String priority) {
        return switch (StringUtils.hasText(priority) ? priority : "") {
            case "critical" -> "紧急";
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> "未知";
        };
    }

    private String buildBridgeLogKey(Long messageId, String channelCode) {
        return messageId + "|" + channelCode;
    }

    private String safeText(Object primary, Object fallback) {
        if (primary != null && StringUtils.hasText(String.valueOf(primary))) {
            return String.valueOf(primary);
        }
        return fallback == null ? "-" : String.valueOf(fallback);
    }

    private String safeText(Object value) {
        return safeText(value, "-");
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(truncated)";
    }
}
