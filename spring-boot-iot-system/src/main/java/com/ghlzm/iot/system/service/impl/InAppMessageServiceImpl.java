package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.entity.InAppMessageRead;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.InAppMessageMapper;
import com.ghlzm.iot.system.mapper.InAppMessageReadMapper;
import com.ghlzm.iot.system.service.InAppMessageService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.vo.InAppMessageAccessVO;
import com.ghlzm.iot.system.vo.InAppMessageStatsVO;
import com.ghlzm.iot.system.vo.InAppMessageUnreadStatsVO;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InAppMessageServiceImpl extends ServiceImpl<InAppMessageMapper, InAppMessage> implements InAppMessageService {

    private static final SimpleDateFormat STATS_TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat STATS_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    private final InAppMessageMapper inAppMessageMapper;
    private final InAppMessageReadMapper inAppMessageReadMapper;
    private final PermissionService permissionService;
    private final UserService userService;
    private final SystemContentSchemaSupport systemContentSchemaSupport;

    public InAppMessageServiceImpl(InAppMessageMapper inAppMessageMapper,
                                   InAppMessageReadMapper inAppMessageReadMapper,
                                   PermissionService permissionService,
                                   UserService userService,
                                   SystemContentSchemaSupport systemContentSchemaSupport) {
        this.inAppMessageMapper = inAppMessageMapper;
        this.inAppMessageReadMapper = inAppMessageReadMapper;
        this.permissionService = permissionService;
        this.userService = userService;
        this.systemContentSchemaSupport = systemContentSchemaSupport;
    }

    @Override
    public InAppMessage getById(Serializable id) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InAppMessage addMessage(InAppMessage message, Long operatorId) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        normalizeAndValidateMessage(message, null, resolveTenantId(operatorId, message == null ? null : message.getTenantId()));
        if (message.getCreateBy() == null) {
            message.setCreateBy(defaultOperator(operatorId));
        }
        if (message.getUpdateBy() == null) {
            message.setUpdateBy(defaultOperator(operatorId));
        }
        InAppMessage existing = findDedupedMessage(message);
        if (existing != null) {
            return existing;
        }
        inAppMessageMapper.insert(message);
        return inAppMessageMapper.selectById(message.getId());
    }

    @Override
    public PageResult<InAppMessage> pageMessages(String title,
                                                 String messageType,
                                                 String priority,
                                                 String sourceType,
                                                 String targetType,
                                                 Integer status,
                                                 Long pageNum,
                                                 Long pageSize) {
        return pageMessages(null, title, messageType, priority, sourceType, targetType, status, pageNum, pageSize);
    }

    @Override
    public PageResult<InAppMessage> pageMessages(Long currentUserId,
                                                 String title,
                                                 String messageType,
                                                 String priority,
                                                 String sourceType,
                                                 String targetType,
                                                 Integer status,
                                                 Long pageNum,
                                                 Long pageSize) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        Page<InAppMessage> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Long tenantId = resolveTenantId(currentUserId, null);
        LambdaQueryWrapper<InAppMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessage::getDeleted, 0)
                .eq(tenantId != null, InAppMessage::getTenantId, tenantId);
        if (StringUtils.hasText(title)) {
            queryWrapper.and(wrapper -> wrapper.like(InAppMessage::getTitle, title.trim())
                    .or()
                    .like(InAppMessage::getSummary, title.trim()));
        }
        if (StringUtils.hasText(messageType)) {
            queryWrapper.eq(InAppMessage::getMessageType, messageType.trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(priority)) {
            queryWrapper.eq(InAppMessage::getPriority, priority.trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(sourceType)) {
            queryWrapper.eq(InAppMessage::getSourceType, sourceType.trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(targetType)) {
            queryWrapper.eq(InAppMessage::getTargetType, targetType.trim().toLowerCase(Locale.ROOT));
        }
        if (status != null) {
            queryWrapper.eq(InAppMessage::getStatus, status);
        }
        queryWrapper.orderByAsc(InAppMessage::getSortNo)
                .orderByDesc(InAppMessage::getPublishTime)
                .orderByDesc(InAppMessage::getId);
        return PageQueryUtils.toPageResult(inAppMessageMapper.selectPage(page, queryWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMessage(InAppMessage message, Long operatorId) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        InAppMessage existing = requireMessage(operatorId, message == null ? null : message.getId());
        normalizeAndValidateMessage(message, existing, existing.getTenantId());
        ensureAutomaticMessageOnlyStatusUpdate(existing, message);
        message.setTenantId(existing.getTenantId());
        message.setUpdateBy(defaultOperator(operatorId));
        inAppMessageMapper.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long id, Long operatorId) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        InAppMessage existing = requireMessage(operatorId, id);
        if (InAppMessageSupport.isAutomaticSourceType(existing.getSourceType())) {
            throw new BizException("系统自动消息只允许查看或停用");
        }
        if (!removeById(id)) {
            throw new BizException("站内消息删除失败");
        }
    }

    @Override
    public PageResult<InAppMessageAccessVO> pageMyMessages(Long userId,
                                                           String messageType,
                                                           Boolean unreadOnly,
                                                           Long pageNum,
                                                           Long pageSize) {
        ensureMyMessageAccessReady();
        long safePageNum = PageQueryUtils.normalizePageNum(pageNum);
        long safePageSize = PageQueryUtils.normalizePageSize(pageSize);
        List<InAppMessage> accessibleMessages = listAccessibleMessages(userId);
        if (StringUtils.hasText(messageType)) {
            String normalizedType = messageType.trim().toLowerCase(Locale.ROOT);
            accessibleMessages = accessibleMessages.stream()
                    .filter(message -> normalizedType.equals(message.getMessageType()))
                    .toList();
        }
        Map<Long, InAppMessageRead> readMap = queryReadMap(
                userId,
                resolveTenantId(userId, null),
                accessibleMessages.stream().map(InAppMessage::getId).toList()
        );
        if (Boolean.TRUE.equals(unreadOnly)) {
            accessibleMessages = accessibleMessages.stream()
                    .filter(message -> !readMap.containsKey(message.getId()))
                    .toList();
        }
        accessibleMessages = accessibleMessages.stream()
                .sorted(accessibleMessageComparator(readMap))
                .toList();
        if (accessibleMessages.isEmpty()) {
            return PageResult.empty(safePageNum, safePageSize);
        }

        int fromIndex = (int) Math.min((safePageNum - 1L) * safePageSize, accessibleMessages.size());
        int toIndex = (int) Math.min(fromIndex + safePageSize, accessibleMessages.size());
        List<InAppMessageAccessVO> records = accessibleMessages.subList(fromIndex, toIndex).stream()
                .map(message -> toAccessVO(message, readMap.get(message.getId())))
                .toList();
        return PageResult.of((long) accessibleMessages.size(), safePageNum, safePageSize, records);
    }

    @Override
    public InAppMessageUnreadStatsVO getMyUnreadStats(Long userId) {
        ensureMyMessageAccessReady();
        List<InAppMessage> accessibleMessages = listAccessibleMessagesForUnreadStats(userId);
        Map<Long, InAppMessageRead> readMap = queryReadMap(
                userId,
                resolveTenantId(userId, null),
                accessibleMessages.stream().map(InAppMessage::getId).toList()
        );
        InAppMessageUnreadStatsVO stats = new InAppMessageUnreadStatsVO();
        for (InAppMessage message : accessibleMessages) {
            if (readMap.containsKey(message.getId())) {
                continue;
            }
            stats.setTotalUnreadCount(stats.getTotalUnreadCount() + 1L);
            switch (message.getMessageType()) {
                case "system" -> stats.setSystemUnreadCount(stats.getSystemUnreadCount() + 1L);
                case "business" -> stats.setBusinessUnreadCount(stats.getBusinessUnreadCount() + 1L);
                case "error" -> stats.setErrorUnreadCount(stats.getErrorUnreadCount() + 1L);
                default -> {
                }
            }
        }
        return stats;
    }

    @Override
    public InAppMessageAccessVO getMyMessageDetail(Long userId, Long id) {
        ensureMyMessageAccessReady();
        InAppMessage message = requireVisibleMessage(userId, id);
        InAppMessageRead readRecord = queryReadRecord(userId, message.getTenantId(), message.getId());
        return toAccessVO(message, readRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessageRead(Long userId, Long id) {
        ensureMyMessageAccessReady();
        InAppMessage message = requireVisibleMessage(userId, id);
        if (queryReadRecord(userId, message.getTenantId(), message.getId()) != null) {
            return;
        }
        insertReadRecord(message, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllMessagesRead(Long userId) {
        ensureMyMessageAccessReady();
        List<InAppMessage> accessibleMessages = listAccessibleMessages(userId);
        if (accessibleMessages.isEmpty()) {
            return;
        }
        Long tenantId = resolveTenantId(userId, null);
        Map<Long, InAppMessageRead> readMap = queryReadMap(
                userId,
                tenantId,
                accessibleMessages.stream().map(InAppMessage::getId).toList()
        );
        for (InAppMessage message : accessibleMessages) {
            if (readMap.containsKey(message.getId())) {
                continue;
            }
            insertReadRecord(message, userId);
        }
    }

    @Override
    public InAppMessageStatsVO getMessageStats(Date startTime,
                                               Date endTime,
                                               String messageType,
                                               String sourceType) {
        return getMessageStats(null, startTime, endTime, messageType, sourceType);
    }

    @Override
    public InAppMessageStatsVO getMessageStats(Long currentUserId,
                                               Date startTime,
                                               Date endTime,
                                               String messageType,
                                               String sourceType) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        ensureMyMessageAccessReady();
        Date[] normalizedRange = normalizeStatsRange(startTime, endTime);
        Long tenantId = resolveTenantId(currentUserId, null);
        List<InAppMessage> messages = listMessagesForStats(tenantId, normalizedRange[0], normalizedRange[1], messageType, sourceType);

        InAppMessageStatsVO stats = new InAppMessageStatsVO();
        stats.setStartTime(normalizedRange[0] == null ? null : STATS_TIME_FORMATTER.format(normalizedRange[0]));
        stats.setEndTime(normalizedRange[1] == null ? null : STATS_TIME_FORMATTER.format(normalizedRange[1]));
        if (messages.isEmpty()) {
            return stats;
        }

        List<User> activeUsers = userService.listUsers(currentUserId, null, null, null, 1);
        Map<Long, User> activeUserMap = activeUsers.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<RoleSummaryVO>> userRoles = permissionService.listUserRolesByUserIds(activeUserMap.keySet());
        Set<String> readKeys = queryReadKeysByMessageIds(tenantId, messages.stream().map(InAppMessage::getId).toList());
        Map<String, InAppMessageStatsVO.TrendBucket> trendBuckets = new LinkedHashMap<>();
        Map<String, InAppMessageStatsVO.Bucket> messageTypeBuckets = new LinkedHashMap<>();
        Map<String, InAppMessageStatsVO.Bucket> sourceTypeBuckets = new LinkedHashMap<>();
        List<InAppMessageStatsVO.TopUnreadMessage> topUnreadMessages = new java.util.ArrayList<>();

        for (InAppMessage message : messages) {
            DeliverySnapshot snapshot = resolveDeliverySnapshot(message, activeUserMap, userRoles, readKeys);
            stats.setTotalDeliveryCount(stats.getTotalDeliveryCount() + snapshot.deliveryCount());
            stats.setTotalReadCount(stats.getTotalReadCount() + snapshot.readCount());
            stats.setTotalUnreadCount(stats.getTotalUnreadCount() + snapshot.unreadCount());

            String trendKey = message.getPublishTime() == null ? "未知日期" : STATS_DATE_FORMATTER.format(message.getPublishTime());
            InAppMessageStatsVO.TrendBucket trendBucket = trendBuckets.computeIfAbsent(trendKey, key -> {
                InAppMessageStatsVO.TrendBucket bucket = new InAppMessageStatsVO.TrendBucket();
                bucket.setDate(key);
                return bucket;
            });
            trendBucket.setDeliveryCount(trendBucket.getDeliveryCount() + snapshot.deliveryCount());
            trendBucket.setReadCount(trendBucket.getReadCount() + snapshot.readCount());
            trendBucket.setUnreadCount(trendBucket.getUnreadCount() + snapshot.unreadCount());

            accumulateBucket(messageTypeBuckets,
                    message.getMessageType(),
                    resolveMessageTypeLabel(message.getMessageType()),
                    snapshot);
            String normalizedSourceType = InAppMessageSupport.normalizeSourceType(message.getSourceType(), "manual");
            accumulateBucket(sourceTypeBuckets,
                    normalizedSourceType,
                    resolveSourceTypeLabel(normalizedSourceType),
                    snapshot);

            InAppMessageStatsVO.TopUnreadMessage topUnreadMessage = new InAppMessageStatsVO.TopUnreadMessage();
            topUnreadMessage.setMessageId(message.getId());
            topUnreadMessage.setTitle(message.getTitle());
            topUnreadMessage.setMessageType(message.getMessageType());
            topUnreadMessage.setSourceType(normalizedSourceType);
            topUnreadMessage.setPublishTime(message.getPublishTime() == null ? null : STATS_TIME_FORMATTER.format(message.getPublishTime()));
            topUnreadMessage.setDeliveryCount(snapshot.deliveryCount());
            topUnreadMessage.setReadCount(snapshot.readCount());
            topUnreadMessage.setUnreadCount(snapshot.unreadCount());
            topUnreadMessage.setUnreadRate(snapshot.deliveryCount() <= 0
                    ? 0D
                    : (double) snapshot.unreadCount() / snapshot.deliveryCount());
            topUnreadMessages.add(topUnreadMessage);
        }

        stats.setReadRate(stats.getTotalDeliveryCount() <= 0
                ? 0D
                : (double) stats.getTotalReadCount() / stats.getTotalDeliveryCount());
        stats.setTrend(trendBuckets.values().stream()
                .sorted(Comparator.comparing(InAppMessageStatsVO.TrendBucket::getDate))
                .toList());
        stats.setMessageTypeBuckets(finalizeBuckets(messageTypeBuckets));
        stats.setSourceTypeBuckets(finalizeBuckets(sourceTypeBuckets));
        stats.setTopUnreadMessages(topUnreadMessages.stream()
                .sorted(Comparator.comparing(InAppMessageStatsVO.TopUnreadMessage::getUnreadRate, Comparator.reverseOrder())
                        .thenComparing(InAppMessageStatsVO.TopUnreadMessage::getUnreadCount, Comparator.reverseOrder())
                        .thenComparing(InAppMessageStatsVO.TopUnreadMessage::getMessageId, Comparator.reverseOrder()))
                .limit(5)
                .toList());
        return stats;
    }

    @Override
    public InAppMessage getById(Long currentUserId, Long id) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        InAppMessage message = requireMessage(id);
        ensureMessageAccessible(currentUserId, message, "站内消息不存在或无权访问");
        return message;
    }

    private void normalizeAndValidateMessage(InAppMessage message, InAppMessage existing, Long tenantId) {
        if (message == null) {
            throw new BizException("站内消息不能为空");
        }
        message.setTenantId(existing == null ? defaultTenantId(tenantId) : existing.getTenantId());
        message.setMessageType(InAppMessageSupport.normalizeEnum(
                message.getMessageType(),
                InAppMessageSupport.ALLOWED_MESSAGE_TYPES,
                "消息类型",
                null
        ));
        message.setPriority(InAppMessageSupport.normalizeEnum(
                message.getPriority(),
                InAppMessageSupport.ALLOWED_PRIORITIES,
                "消息优先级",
                "medium"
        ));
        message.setTargetType(InAppMessageSupport.normalizeEnum(
                message.getTargetType(),
                InAppMessageSupport.ALLOWED_TARGET_TYPES,
                "推送范围",
                "all"
        ));
        message.setTitle(InAppMessageSupport.requireText(message.getTitle(), "消息标题"));
        message.setSummary(InAppMessageSupport.nullableText(message.getSummary()));
        message.setContent(InAppMessageSupport.nullableText(message.getContent()));
        if (!StringUtils.hasText(message.getSummary()) && !StringUtils.hasText(message.getContent())) {
            throw new BizException("消息摘要和正文不能同时为空");
        }

        if ("role".equals(message.getTargetType())) {
            String normalizedRoles = SystemContentAccessSupport.normalizeUpperCaseCsv(message.getTargetRoleCodes());
            if (!StringUtils.hasText(normalizedRoles)) {
                throw new BizException("按角色推送时必须指定目标角色");
            }
            message.setTargetRoleCodes(normalizedRoles);
            message.setTargetUserIds(null);
        } else if ("user".equals(message.getTargetType())) {
            String normalizedUserIds = InAppMessageSupport.normalizeUserIdsCsv(message.getTargetUserIds());
            if (!StringUtils.hasText(normalizedUserIds)) {
                throw new BizException("按用户推送时必须指定目标用户");
            }
            message.setTargetUserIds(normalizedUserIds);
            message.setTargetRoleCodes(null);
        } else {
            message.setTargetRoleCodes(null);
            message.setTargetUserIds(null);
        }

        message.setRelatedPath(SystemContentAccessSupport.normalizePath(message.getRelatedPath()));
        message.setSourceType(InAppMessageSupport.normalizeSourceType(
                message.getSourceType(),
                existing == null ? "manual" : existing.getSourceType()
        ));
        message.setSourceId(InAppMessageSupport.nullableText(message.getSourceId()));
        message.setPublishTime(message.getPublishTime() == null ? new Date() : message.getPublishTime());
        message.setExpireTime(InAppMessageSupport.resolveDefaultExpireTime(
                message.getSourceType(),
                message.getPublishTime(),
                message.getExpireTime()
        ));
        if (message.getExpireTime() != null && !message.getExpireTime().after(message.getPublishTime())) {
            throw new BizException("过期时间必须晚于发布时间");
        }
        if (message.getStatus() == null) {
            message.setStatus(existing == null ? 1 : existing.getStatus());
        }
        if (message.getSortNo() == null) {
            message.setSortNo(existing == null ? 0 : existing.getSortNo());
        }
        message.setDedupKey(InAppMessageSupport.buildDedupKey(message));
    }

    private List<InAppMessage> listAccessibleMessages(Long userId) {
        return listAccessibleMessages(userId, false, true);
    }

    private List<InAppMessage> listAccessibleMessagesForUnreadStats(Long userId) {
        return listAccessibleMessages(userId, true, false);
    }

    private List<InAppMessage> listAccessibleMessages(Long userId, boolean lightweightSelect, boolean ordered) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        UserAuthContextVO authContext = permissionService.getUserAuthContext(userId);
        Set<String> roleCodes = SystemContentAccessSupport.toUpperCaseSet(authContext.getRoleCodes());
        Long tenantId = resolveTenantId(userId, authContext.getTenantId());
        Date now = new Date();
        java.util.stream.Stream<InAppMessage> accessibleStream = (lightweightSelect
                ? inAppMessageMapper.selectList(buildAccessibleMessageSummaryQuery(tenantId, now))
                : inAppMessageMapper.selectList(buildAccessibleMessageQuery(tenantId, now, ordered))).stream()
                .filter(message -> matchesTarget(message, userId, roleCodes))
                .filter(Objects::nonNull);
        if (ordered) {
            accessibleStream = accessibleStream.sorted(accessibleMessageComparator());
        }
        return accessibleStream.toList();
    }

    private LambdaQueryWrapper<InAppMessage> buildAccessibleMessageQuery(Long tenantId, Date now, boolean ordered) {
        LambdaQueryWrapper<InAppMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessage::getDeleted, 0)
                .eq(tenantId != null, InAppMessage::getTenantId, tenantId)
                .eq(InAppMessage::getStatus, 1)
                .le(InAppMessage::getPublishTime, now)
                .and(wrapper -> wrapper.isNull(InAppMessage::getExpireTime)
                        .or()
                        .gt(InAppMessage::getExpireTime, now));
        if (ordered) {
            queryWrapper.orderByAsc(InAppMessage::getSortNo)
                    .orderByDesc(InAppMessage::getPublishTime)
                    .orderByDesc(InAppMessage::getId);
        }
        return queryWrapper;
    }

    private QueryWrapper<InAppMessage> buildAccessibleMessageSummaryQuery(Long tenantId, Date now) {
        QueryWrapper<InAppMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "message_type", "target_type", "target_role_codes", "target_user_ids");
        // 壳层未读角标不需要正文和排序，只取最小字段集以减少后台自动刷新对主库的占用。
        queryWrapper.lambda()
                .eq(InAppMessage::getDeleted, 0)
                .eq(tenantId != null, InAppMessage::getTenantId, tenantId)
                .eq(InAppMessage::getStatus, 1)
                .le(InAppMessage::getPublishTime, now)
                .and(wrapper -> wrapper.isNull(InAppMessage::getExpireTime)
                        .or()
                        .gt(InAppMessage::getExpireTime, now));
        return queryWrapper;
    }

    private boolean matchesTarget(InAppMessage message, Long userId, Set<String> roleCodes) {
        if (message == null || !StringUtils.hasText(message.getTargetType())) {
            return false;
        }
        return switch (message.getTargetType()) {
            case "all" -> true;
            case "role" -> !roleCodes.isEmpty() && SystemContentAccessSupport.splitCsv(message.getTargetRoleCodes()).stream()
                    .map(value -> value.toUpperCase(Locale.ROOT))
                    .anyMatch(roleCodes::contains);
            case "user" -> SystemContentAccessSupport.splitCsv(message.getTargetUserIds()).contains(String.valueOf(userId));
            default -> false;
        };
    }

    private Comparator<InAppMessage> accessibleMessageComparator() {
        return Comparator.comparingInt((InAppMessage message) -> priorityWeight(message.getPriority())).reversed()
                .thenComparing(InAppMessage::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(InAppMessage::getSortNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InAppMessage::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<InAppMessage> accessibleMessageComparator(Map<Long, InAppMessageRead> readMap) {
        return Comparator.comparing((InAppMessage message) -> readMap.containsKey(message.getId()))
                .thenComparing(accessibleMessageComparator());
    }

    private int priorityWeight(String priority) {
        return switch (priority) {
            case "critical" -> 4;
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private Map<Long, InAppMessageRead> queryReadMap(Long userId, Long tenantId, Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<InAppMessageRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessageRead::getUserId, userId)
                .eq(tenantId != null, InAppMessageRead::getTenantId, tenantId)
                .in(InAppMessageRead::getMessageId, messageIds);
        return inAppMessageReadMapper.selectList(queryWrapper).stream()
                .filter(item -> item.getMessageId() != null)
                .collect(Collectors.toMap(InAppMessageRead::getMessageId, Function.identity(), (left, right) -> left));
    }

    private Set<String> queryReadKeysByMessageIds(Long tenantId, Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<InAppMessageRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(tenantId != null, InAppMessageRead::getTenantId, tenantId)
                .in(InAppMessageRead::getMessageId, messageIds);
        return inAppMessageReadMapper.selectList(queryWrapper).stream()
                .filter(item -> item.getMessageId() != null && item.getUserId() != null)
                .map(item -> buildReadKey(item.getMessageId(), item.getUserId()))
                .collect(Collectors.toSet());
    }

    private InAppMessageRead queryReadRecord(Long userId, Long tenantId, Long messageId) {
        LambdaQueryWrapper<InAppMessageRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessageRead::getUserId, userId)
                .eq(tenantId != null, InAppMessageRead::getTenantId, tenantId)
                .eq(InAppMessageRead::getMessageId, messageId)
                .last("LIMIT 1");
        return inAppMessageReadMapper.selectOne(queryWrapper);
    }

    private void insertReadRecord(InAppMessage message, Long userId) {
        InAppMessageRead readRecord = new InAppMessageRead();
        readRecord.setTenantId(defaultTenantId(message.getTenantId()));
        readRecord.setMessageId(message.getId());
        readRecord.setUserId(userId);
        Date now = new Date();
        readRecord.setReadTime(now);
        readRecord.setCreateTime(now);
        readRecord.setUpdateTime(now);
        inAppMessageReadMapper.insert(readRecord);
    }

    private InAppMessage requireVisibleMessage(Long userId, Long id) {
        InAppMessage message = requireMessage(id);
        Date now = new Date();
        if (!Integer.valueOf(1).equals(message.getStatus())
                || (message.getPublishTime() != null && message.getPublishTime().after(now))
                || (message.getExpireTime() != null && !message.getExpireTime().after(now))) {
            throw new BizException("站内消息不存在");
        }
        ensureMessageAccessible(userId, message, "站内消息不存在");
        UserAuthContextVO authContext = permissionService.getUserAuthContext(userId);
        if (!matchesTarget(message, userId, SystemContentAccessSupport.toUpperCaseSet(authContext.getRoleCodes()))) {
            throw new BizException("站内消息不存在");
        }
        return message;
    }

    private InAppMessage requireMessage(Long currentUserId, Long id) {
        InAppMessage message = requireMessage(id);
        ensureMessageAccessible(currentUserId, message, "站内消息不存在或无权访问");
        return message;
    }

    private InAppMessage requireMessage(Long id) {
        if (id == null) {
            throw new BizException("站内消息不存在");
        }
        InAppMessage message = inAppMessageMapper.selectById(id);
        if (message == null || Integer.valueOf(1).equals(message.getDeleted())) {
            throw new BizException("站内消息不存在");
        }
        return message;
    }

    private InAppMessageAccessVO toAccessVO(InAppMessage message, InAppMessageRead readRecord) {
        InAppMessageAccessVO vo = new InAppMessageAccessVO();
        vo.setId(message.getId());
        vo.setMessageType(message.getMessageType());
        vo.setPriority(message.getPriority());
        vo.setTitle(message.getTitle());
        vo.setSummary(message.getSummary());
        vo.setContent(message.getContent());
        vo.setTargetType(message.getTargetType());
        vo.setRelatedPath(message.getRelatedPath());
        vo.setSourceType(message.getSourceType());
        vo.setSourceId(message.getSourceId());
        vo.setPublishTime(message.getPublishTime());
        vo.setExpireTime(message.getExpireTime());
        vo.setRead(readRecord != null);
        vo.setReadTime(readRecord == null ? null : readRecord.getReadTime());
        return vo;
    }

    private void ensureAutomaticMessageOnlyStatusUpdate(InAppMessage existing, InAppMessage updating) {
        if (!InAppMessageSupport.isAutomaticSourceType(existing == null ? null : existing.getSourceType())) {
            return;
        }
        boolean onlyStatusChanged = Objects.equals(existing.getMessageType(), updating.getMessageType())
                && Objects.equals(existing.getPriority(), updating.getPriority())
                && Objects.equals(existing.getTitle(), updating.getTitle())
                && Objects.equals(existing.getSummary(), updating.getSummary())
                && Objects.equals(existing.getContent(), updating.getContent())
                && Objects.equals(existing.getTargetType(), updating.getTargetType())
                && Objects.equals(existing.getTargetRoleCodes(), updating.getTargetRoleCodes())
                && Objects.equals(existing.getTargetUserIds(), updating.getTargetUserIds())
                && Objects.equals(existing.getRelatedPath(), updating.getRelatedPath())
                && Objects.equals(existing.getSourceType(), updating.getSourceType())
                && Objects.equals(existing.getSourceId(), updating.getSourceId())
                && Objects.equals(existing.getPublishTime(), updating.getPublishTime())
                && Objects.equals(existing.getExpireTime(), updating.getExpireTime())
                && Objects.equals(existing.getSortNo(), updating.getSortNo());
        if (!onlyStatusChanged) {
            throw new BizException("系统自动消息只允许查看或停用");
        }
    }

    private InAppMessage findDedupedMessage(InAppMessage message) {
        if (message == null || !StringUtils.hasText(message.getDedupKey())) {
            return null;
        }
        Date now = new Date();
        LambdaQueryWrapper<InAppMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessage::getTenantId, message.getTenantId())
                .eq(InAppMessage::getDedupKey, message.getDedupKey())
                .eq(InAppMessage::getDeleted, 0)
                .eq(InAppMessage::getStatus, 1)
                .and(wrapper -> wrapper.isNull(InAppMessage::getExpireTime)
                        .or()
                        .gt(InAppMessage::getExpireTime, now))
                .orderByDesc(InAppMessage::getPublishTime)
                .orderByDesc(InAppMessage::getId)
                .last("LIMIT 1");
        return inAppMessageMapper.selectOne(queryWrapper);
    }

    private List<InAppMessage> listMessagesForStats(Date startTime,
                                                    Date endTime,
                                                    String messageType,
                                                    String sourceType) {
        return listMessagesForStats(null, startTime, endTime, messageType, sourceType);
    }

    private List<InAppMessage> listMessagesForStats(Long tenantId,
                                                    Date startTime,
                                                    Date endTime,
                                                    String messageType,
                                                    String sourceType) {
        LambdaQueryWrapper<InAppMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessage::getDeleted, 0)
                .eq(tenantId != null, InAppMessage::getTenantId, tenantId);
        if (startTime != null) {
            queryWrapper.ge(InAppMessage::getPublishTime, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(InAppMessage::getPublishTime, endTime);
        }
        if (StringUtils.hasText(messageType)) {
            queryWrapper.eq(InAppMessage::getMessageType, messageType.trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(sourceType)) {
            queryWrapper.eq(InAppMessage::getSourceType, sourceType.trim().toLowerCase(Locale.ROOT));
        }
        queryWrapper.orderByDesc(InAppMessage::getPublishTime)
                .orderByDesc(InAppMessage::getId);
        return inAppMessageMapper.selectList(queryWrapper);
    }

    private Date[] normalizeStatsRange(Date startTime, Date endTime) {
        Date normalizedEndTime = endTime == null ? new Date() : endTime;
        Date normalizedStartTime = startTime == null
                ? new Date(normalizedEndTime.getTime() - 6L * 24L * 60L * 60L * 1000L)
                : startTime;
        return new Date[]{normalizedStartTime, normalizedEndTime};
    }

    private DeliverySnapshot resolveDeliverySnapshot(InAppMessage message,
                                                     Map<Long, User> activeUserMap,
                                                     Map<Long, List<RoleSummaryVO>> userRoles,
                                                     Set<String> readKeys) {
        if (message == null || activeUserMap.isEmpty()) {
            return new DeliverySnapshot(0L, 0L, 0L);
        }
        List<Long> targetUserIds = InAppMessageDeliverySupport.resolveTargetUserIds(message, activeUserMap, userRoles);
        long deliveryCount = targetUserIds.size();
        long readCount = targetUserIds.stream()
                .filter(userId -> readKeys.contains(InAppMessageDeliverySupport.buildReadKey(message.getId(), userId)))
                .count();
        return new DeliverySnapshot(deliveryCount, readCount, Math.max(0L, deliveryCount - readCount));
    }

    private void accumulateBucket(Map<String, InAppMessageStatsVO.Bucket> buckets,
                                  String key,
                                  String label,
                                  DeliverySnapshot snapshot) {
        String normalizedKey = StringUtils.hasText(key) ? key : "unknown";
        InAppMessageStatsVO.Bucket bucket = buckets.computeIfAbsent(normalizedKey, ignored -> {
            InAppMessageStatsVO.Bucket item = new InAppMessageStatsVO.Bucket();
            item.setKey(normalizedKey);
            item.setLabel(label);
            return item;
        });
        bucket.setDeliveryCount(bucket.getDeliveryCount() + snapshot.deliveryCount());
        bucket.setReadCount(bucket.getReadCount() + snapshot.readCount());
        bucket.setUnreadCount(bucket.getUnreadCount() + snapshot.unreadCount());
    }

    private List<InAppMessageStatsVO.Bucket> finalizeBuckets(Map<String, InAppMessageStatsVO.Bucket> buckets) {
        return buckets.values().stream()
                .peek(bucket -> bucket.setReadRate(bucket.getDeliveryCount() <= 0
                        ? 0D
                        : (double) bucket.getReadCount() / bucket.getDeliveryCount()))
                .sorted(Comparator.comparing(InAppMessageStatsVO.Bucket::getDeliveryCount, Comparator.reverseOrder()))
                .toList();
    }

    private String resolveMessageTypeLabel(String messageType) {
        return switch (messageType) {
            case "system" -> "系统事件";
            case "business" -> "业务事件";
            case "error" -> "错误事件";
            default -> "站内消息";
        };
    }

    private String resolveSourceTypeLabel(String sourceType) {
        return switch (StringUtils.hasText(sourceType) ? sourceType : "unknown") {
            case "manual" -> "手工广播";
            case "system_maintenance", "daily_report" -> "手工广播";
            case "system_error" -> "系统异常";
            case "event_dispatch" -> "事件派工";
            case "work_order" -> "工单状态";
            case "governance", "governance_task" -> "治理任务";
            default -> sourceType;
        };
    }

    private String buildReadKey(Long messageId, Long userId) {
        return InAppMessageDeliverySupport.buildReadKey(messageId, userId);
    }

    private Long resolveTenantId(Long currentUserId, Long fallbackTenantId) {
        if (currentUserId == null) {
            return fallbackTenantId;
        }
        DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
        return context == null ? fallbackTenantId : context.tenantId();
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null ? InAppMessageSupport.DEFAULT_TENANT_ID : tenantId;
    }

    private Long defaultOperator(Long operatorId) {
        return operatorId == null ? InAppMessageSupport.DEFAULT_OPERATOR_ID : operatorId;
    }

    private void ensureMyMessageAccessReady() {
        systemContentSchemaSupport.ensureInAppMessageReady();
        systemContentSchemaSupport.ensureInAppMessageReadReady();
    }

    private void ensureMessageAccessible(Long currentUserId, InAppMessage message, String errorMessage) {
        if (currentUserId == null || message == null) {
            return;
        }
        DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return;
        }
        if (context.tenantId() != null && !context.tenantId().equals(message.getTenantId())) {
            throw new BizException(errorMessage);
        }
    }

    private record DeliverySnapshot(long deliveryCount, long readCount, long unreadCount) {
    }
}
