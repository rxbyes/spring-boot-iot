package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.entity.InAppMessageRead;
import com.ghlzm.iot.system.mapper.InAppMessageMapper;
import com.ghlzm.iot.system.mapper.InAppMessageReadMapper;
import com.ghlzm.iot.system.service.InAppMessageService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.InAppMessageAccessVO;
import com.ghlzm.iot.system.vo.InAppMessageUnreadStatsVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InAppMessageServiceImpl extends ServiceImpl<InAppMessageMapper, InAppMessage> implements InAppMessageService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Set<String> ALLOWED_MESSAGE_TYPES = Set.of("system", "business", "error");
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("critical", "high", "medium", "low");
    private static final Set<String> ALLOWED_TARGET_TYPES = Set.of("all", "role", "user");

    private final InAppMessageMapper inAppMessageMapper;
    private final InAppMessageReadMapper inAppMessageReadMapper;
    private final PermissionService permissionService;

    public InAppMessageServiceImpl(InAppMessageMapper inAppMessageMapper,
                                   InAppMessageReadMapper inAppMessageReadMapper,
                                   PermissionService permissionService) {
        this.inAppMessageMapper = inAppMessageMapper;
        this.inAppMessageReadMapper = inAppMessageReadMapper;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InAppMessage addMessage(InAppMessage message, Long operatorId) {
        normalizeAndValidateMessage(message, null);
        if (message.getCreateBy() == null) {
            message.setCreateBy(defaultOperator(operatorId));
        }
        if (message.getUpdateBy() == null) {
            message.setUpdateBy(defaultOperator(operatorId));
        }
        inAppMessageMapper.insert(message);
        return inAppMessageMapper.selectById(message.getId());
    }

    @Override
    public PageResult<InAppMessage> pageMessages(String title,
                                                 String messageType,
                                                 String priority,
                                                 String targetType,
                                                 Integer status,
                                                 Long pageNum,
                                                 Long pageSize) {
        Page<InAppMessage> page = PageQueryUtils.buildPage(pageNum, pageSize);
        LambdaQueryWrapper<InAppMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessage::getDeleted, 0);
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
        InAppMessage existing = requireMessage(message == null ? null : message.getId());
        normalizeAndValidateMessage(message, existing);
        message.setTenantId(existing.getTenantId());
        message.setUpdateBy(defaultOperator(operatorId));
        inAppMessageMapper.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long id, Long operatorId) {
        InAppMessage existing = requireMessage(id);
        existing.setDeleted(1);
        existing.setUpdateBy(defaultOperator(operatorId));
        inAppMessageMapper.updateById(existing);
    }

    @Override
    public PageResult<InAppMessageAccessVO> pageMyMessages(Long userId,
                                                           String messageType,
                                                           Boolean unreadOnly,
                                                           Long pageNum,
                                                           Long pageSize) {
        long safePageNum = PageQueryUtils.normalizePageNum(pageNum);
        long safePageSize = PageQueryUtils.normalizePageSize(pageSize);
        List<InAppMessage> accessibleMessages = listAccessibleMessages(userId);
        if (StringUtils.hasText(messageType)) {
            String normalizedType = messageType.trim().toLowerCase(Locale.ROOT);
            accessibleMessages = accessibleMessages.stream()
                    .filter(message -> normalizedType.equals(message.getMessageType()))
                    .toList();
        }
        Map<Long, InAppMessageRead> readMap = queryReadMap(userId, accessibleMessages.stream().map(InAppMessage::getId).toList());
        if (Boolean.TRUE.equals(unreadOnly)) {
            accessibleMessages = accessibleMessages.stream()
                    .filter(message -> !readMap.containsKey(message.getId()))
                    .toList();
        }
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
        List<InAppMessage> accessibleMessages = listAccessibleMessages(userId);
        Map<Long, InAppMessageRead> readMap = queryReadMap(userId, accessibleMessages.stream().map(InAppMessage::getId).toList());
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
        InAppMessage message = requireVisibleMessage(userId, id);
        InAppMessageRead readRecord = queryReadRecord(userId, message.getId());
        return toAccessVO(message, readRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessageRead(Long userId, Long id) {
        InAppMessage message = requireVisibleMessage(userId, id);
        if (queryReadRecord(userId, message.getId()) != null) {
            return;
        }
        insertReadRecord(message, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllMessagesRead(Long userId) {
        List<InAppMessage> accessibleMessages = listAccessibleMessages(userId);
        if (accessibleMessages.isEmpty()) {
            return;
        }
        Map<Long, InAppMessageRead> readMap = queryReadMap(userId, accessibleMessages.stream().map(InAppMessage::getId).toList());
        for (InAppMessage message : accessibleMessages) {
            if (readMap.containsKey(message.getId())) {
                continue;
            }
            insertReadRecord(message, userId);
        }
    }

    private void normalizeAndValidateMessage(InAppMessage message, InAppMessage existing) {
        if (message == null) {
            throw new BizException("站内消息不能为空");
        }
        message.setTenantId(existing == null ? defaultTenantId(message.getTenantId()) : existing.getTenantId());
        message.setMessageType(normalizeEnum(message.getMessageType(), ALLOWED_MESSAGE_TYPES, "消息类型", null));
        message.setPriority(normalizeEnum(message.getPriority(), ALLOWED_PRIORITIES, "消息优先级", "medium"));
        message.setTargetType(normalizeEnum(message.getTargetType(), ALLOWED_TARGET_TYPES, "推送范围", "all"));
        message.setTitle(requireText(message.getTitle(), "消息标题"));
        message.setSummary(nullableText(message.getSummary()));
        message.setContent(nullableText(message.getContent()));
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
            String normalizedUserIds = normalizeUserIdsCsv(message.getTargetUserIds());
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
        message.setSourceType(nullableText(message.getSourceType()));
        message.setSourceId(nullableText(message.getSourceId()));
        message.setPublishTime(message.getPublishTime() == null ? new Date() : message.getPublishTime());
        if (message.getExpireTime() != null && !message.getExpireTime().after(message.getPublishTime())) {
            throw new BizException("过期时间必须晚于发布时间");
        }
        if (message.getStatus() == null) {
            message.setStatus(existing == null ? 1 : existing.getStatus());
        }
        if (message.getSortNo() == null) {
            message.setSortNo(existing == null ? 0 : existing.getSortNo());
        }
    }

    private List<InAppMessage> listAccessibleMessages(Long userId) {
        UserAuthContextVO authContext = permissionService.getUserAuthContext(userId);
        Set<String> roleCodes = SystemContentAccessSupport.toUpperCaseSet(authContext.getRoleCodes());
        Date now = new Date();
        LambdaQueryWrapper<InAppMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessage::getDeleted, 0)
                .eq(InAppMessage::getStatus, 1)
                .le(InAppMessage::getPublishTime, now)
                .and(wrapper -> wrapper.isNull(InAppMessage::getExpireTime)
                        .or()
                        .gt(InAppMessage::getExpireTime, now))
                .orderByAsc(InAppMessage::getSortNo)
                .orderByDesc(InAppMessage::getPublishTime)
                .orderByDesc(InAppMessage::getId);
        return inAppMessageMapper.selectList(queryWrapper).stream()
                .filter(message -> matchesTarget(message, userId, roleCodes))
                .sorted(accessibleMessageComparator())
                .toList();
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

    private int priorityWeight(String priority) {
        return switch (priority) {
            case "critical" -> 4;
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private Map<Long, InAppMessageRead> queryReadMap(Long userId, Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<InAppMessageRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessageRead::getUserId, userId)
                .in(InAppMessageRead::getMessageId, messageIds);
        return inAppMessageReadMapper.selectList(queryWrapper).stream()
                .filter(item -> item.getMessageId() != null)
                .collect(Collectors.toMap(InAppMessageRead::getMessageId, Function.identity(), (left, right) -> left));
    }

    private InAppMessageRead queryReadRecord(Long userId, Long messageId) {
        LambdaQueryWrapper<InAppMessageRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InAppMessageRead::getUserId, userId)
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
        UserAuthContextVO authContext = permissionService.getUserAuthContext(userId);
        if (!matchesTarget(message, userId, SystemContentAccessSupport.toUpperCaseSet(authContext.getRoleCodes()))) {
            throw new BizException("站内消息不存在");
        }
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

    private String normalizeEnum(String raw,
                                 Set<String> allowedValues,
                                 String fieldName,
                                 String defaultValue) {
        String normalized = StringUtils.hasText(raw) ? raw.trim().toLowerCase(Locale.ROOT) : defaultValue;
        if (!StringUtils.hasText(normalized) || !allowedValues.contains(normalized)) {
            throw new BizException(fieldName + "不合法");
        }
        return normalized;
    }

    private String requireText(String raw, String fieldName) {
        String normalized = nullableText(raw);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String nullableText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }

    private String normalizeUserIdsCsv(String raw) {
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

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private Long defaultOperator(Long operatorId) {
        return operatorId == null ? 1L : operatorId;
    }
}
