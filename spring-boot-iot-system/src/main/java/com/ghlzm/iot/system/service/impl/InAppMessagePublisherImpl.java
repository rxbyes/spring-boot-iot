package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.notification.InAppMessagePublishCommand;
import com.ghlzm.iot.framework.notification.InAppMessagePublishResult;
import com.ghlzm.iot.framework.notification.InAppMessagePublisher;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.mapper.InAppMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 站内消息内部发布服务实现。
 */
@Service
public class InAppMessagePublisherImpl implements InAppMessagePublisher {

    private final InAppMessageMapper inAppMessageMapper;
    private final SystemContentSchemaSupport systemContentSchemaSupport;

    public InAppMessagePublisherImpl(InAppMessageMapper inAppMessageMapper,
                                     SystemContentSchemaSupport systemContentSchemaSupport) {
        this.inAppMessageMapper = inAppMessageMapper;
        this.systemContentSchemaSupport = systemContentSchemaSupport;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InAppMessagePublishResult publish(InAppMessagePublishCommand command) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        InAppMessage message = normalizeCommand(command);
        InAppMessage existing = findDedupedMessage(message);
        if (existing != null) {
            return InAppMessagePublishResult.builder()
                    .messageId(existing.getId())
                    .dedupKey(existing.getDedupKey())
                    .dedupKeyHit(true)
                    .created(false)
                    .build();
        }
        inAppMessageMapper.insert(message);
        return InAppMessagePublishResult.builder()
                .messageId(message.getId())
                .dedupKey(message.getDedupKey())
                .dedupKeyHit(false)
                .created(true)
                .build();
    }

    private InAppMessage normalizeCommand(InAppMessagePublishCommand command) {
        if (command == null) {
            throw new BizException("站内消息发布命令不能为空");
        }
        InAppMessage message = new InAppMessage();
        message.setTenantId(command.getTenantId() == null ? InAppMessageSupport.DEFAULT_TENANT_ID : command.getTenantId());
        message.setMessageType(InAppMessageSupport.normalizeEnum(
                command.getMessageType(),
                InAppMessageSupport.ALLOWED_MESSAGE_TYPES,
                "消息类型",
                null
        ));
        message.setPriority(InAppMessageSupport.normalizeEnum(
                command.getPriority(),
                InAppMessageSupport.ALLOWED_PRIORITIES,
                "消息优先级",
                "medium"
        ));
        message.setTitle(InAppMessageSupport.requireText(command.getTitle(), "消息标题"));
        message.setSummary(InAppMessageSupport.nullableText(command.getSummary()));
        message.setContent(InAppMessageSupport.nullableText(command.getContent()));
        if (!StringUtils.hasText(message.getSummary()) && !StringUtils.hasText(message.getContent())) {
            throw new BizException("消息摘要和正文不能同时为空");
        }

        message.setTargetType(InAppMessageSupport.normalizeEnum(
                command.getTargetType(),
                InAppMessageSupport.ALLOWED_TARGET_TYPES,
                "推送范围",
                "all"
        ));
        if ("role".equals(message.getTargetType())) {
            String targetRoleCodes = CollectionUtils.isEmpty(command.getTargetRoleCodes())
                    ? null
                    : command.getTargetRoleCodes().stream()
                    .filter(StringUtils::hasText)
                    .map(value -> value.trim().toUpperCase(Locale.ROOT))
                    .distinct()
                    .collect(Collectors.joining(","));
            if (!StringUtils.hasText(targetRoleCodes)) {
                throw new BizException("按角色推送时必须指定目标角色");
            }
            message.setTargetRoleCodes(targetRoleCodes);
            message.setTargetUserIds(null);
        } else if ("user".equals(message.getTargetType())) {
            String targetUserIds = CollectionUtils.isEmpty(command.getTargetUserIds())
                    ? null
                    : command.getTargetUserIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .distinct()
                    .collect(Collectors.joining(","));
            targetUserIds = InAppMessageSupport.normalizeUserIdsCsv(targetUserIds);
            if (!StringUtils.hasText(targetUserIds)) {
                throw new BizException("按用户推送时必须指定目标用户");
            }
            message.setTargetRoleCodes(null);
            message.setTargetUserIds(targetUserIds);
        } else {
            message.setTargetRoleCodes(null);
            message.setTargetUserIds(null);
        }

        message.setRelatedPath(SystemContentAccessSupport.normalizePath(command.getRelatedPath()));
        message.setSourceType(InAppMessageSupport.normalizeSourceType(command.getSourceType(), "manual"));
        message.setSourceId(InAppMessageSupport.nullableText(command.getSourceId()));
        message.setPublishTime(command.getPublishTime() == null ? new Date() : command.getPublishTime());
        message.setExpireTime(InAppMessageSupport.resolveDefaultExpireTime(
                message.getSourceType(),
                message.getPublishTime(),
                command.getExpireTime()
        ));
        if (message.getExpireTime() != null && !message.getExpireTime().after(message.getPublishTime())) {
            throw new BizException("过期时间必须晚于发布时间");
        }
        message.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        message.setSortNo(command.getSortNo() == null ? 0 : command.getSortNo());
        message.setCreateBy(command.getOperatorId() == null ? InAppMessageSupport.DEFAULT_OPERATOR_ID : command.getOperatorId());
        message.setUpdateBy(command.getOperatorId() == null ? InAppMessageSupport.DEFAULT_OPERATOR_ID : command.getOperatorId());
        message.setDeleted(0);
        message.setDedupKey(InAppMessageSupport.buildDedupKey(message));
        return message;
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
}
