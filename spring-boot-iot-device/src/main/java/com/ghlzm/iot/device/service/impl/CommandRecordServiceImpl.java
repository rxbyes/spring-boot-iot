package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.enums.CommandStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.mapper.CommandRecordMapper;
import com.ghlzm.iot.device.service.CommandRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 命令记录服务最小实现。
 * 当前阶段维护 CREATED、SENT、SUCCESS、FAILED、TIMEOUT 的最小状态更新。
 */
@Service
public class CommandRecordServiceImpl implements CommandRecordService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final CommandRecordMapper commandRecordMapper;

    public CommandRecordServiceImpl(CommandRecordMapper commandRecordMapper) {
        this.commandRecordMapper = commandRecordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommandRecord create(CommandRecord commandRecord) {
        if (commandRecord == null) {
            throw new BizException("命令记录不能为空");
        }
        if (!hasText(commandRecord.getCommandId())) {
            throw new BizException("命令记录缺少 commandId");
        }
        if (!hasText(commandRecord.getDeviceCode())) {
            throw new BizException("命令记录缺少 deviceCode");
        }
        commandRecord.setStatus(CommandStatusEnum.CREATED.getCode());
        commandRecord.setErrorMessage(null);
        commandRecordMapper.insert(commandRecord);
        return commandRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSent(Long recordId, LocalDateTime sendTime) {
        updateStatus(recordId, CommandStatusEnum.SENT.getCode(), sendTime, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long recordId, String errorMessage) {
        updateStatus(recordId, CommandStatusEnum.FAILED.getCode(), null, trimErrorMessage(errorMessage));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccessByCommandId(String commandId, String replyPayload, LocalDateTime ackTime) {
        return updateReplyStatus(commandId, CommandStatusEnum.SUCCESS.getCode(), ackTime, null, replyPayload, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markFailedByCommandId(String commandId,
                                         String replyPayload,
                                         String errorMessage,
                                         LocalDateTime ackTime) {
        return updateReplyStatus(
                commandId,
                CommandStatusEnum.FAILED.getCode(),
                ackTime,
                null,
                replyPayload,
                trimErrorMessage(errorMessage)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markTimeout(String commandId, LocalDateTime timeoutTime) {
        return updateReplyStatus(commandId, CommandStatusEnum.TIMEOUT.getCode(), null, timeoutTime, null, null);
    }

    private void updateStatus(Long recordId, String status, LocalDateTime sendTime, String errorMessage) {
        if (recordId == null) {
            throw new BizException("命令记录ID不能为空");
        }

        CommandRecord update = new CommandRecord();
        update.setId(recordId);
        update.setStatus(status);
        update.setSendTime(sendTime);
        update.setErrorMessage(errorMessage);

        if (commandRecordMapper.updateById(update) != 1) {
            throw new BizException("命令记录更新失败: " + recordId);
        }
    }

    private boolean updateReplyStatus(String commandId,
                                      String status,
                                      LocalDateTime ackTime,
                                      LocalDateTime timeoutTime,
                                      String replyPayload,
                                      String errorMessage) {
        if (!hasText(commandId)) {
            throw new BizException("命令记录缺少 commandId");
        }

        CommandRecord existing = commandRecordMapper.selectOne(
                new LambdaQueryWrapper<CommandRecord>()
                        .eq(CommandRecord::getCommandId, commandId)
                        .eq(CommandRecord::getDeleted, 0)
                        .last("limit 1")
        );
        if (existing == null) {
            return false;
        }

        CommandRecord update = new CommandRecord();
        update.setId(existing.getId());
        update.setStatus(status);
        update.setAckTime(ackTime);
        update.setTimeoutTime(timeoutTime);
        update.setReplyPayload(replyPayload);
        update.setErrorMessage(errorMessage);
        if (commandRecordMapper.updateById(update) != 1) {
            throw new BizException("命令记录更新失败: " + existing.getId());
        }
        return true;
    }

    private String trimErrorMessage(String errorMessage) {
        if (!hasText(errorMessage)) {
            return null;
        }
        if (errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
