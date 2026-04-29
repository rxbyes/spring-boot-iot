package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import com.ghlzm.iot.common.response.PageResult;

import java.time.LocalDateTime;

/**
 * 命令记录服务。
 * 当前阶段只负责最小命令记录写入与状态更新，不扩展 ACK 或查询能力。
 */
public interface CommandRecordService {

    /**
     * 创建命令记录，初始状态固定为 CREATED。
     */
    CommandRecord create(CommandRecord commandRecord);

    /**
     * 标记命令已发送。
     */
    void markSent(Long recordId, LocalDateTime sendTime);

    /**
     * 标记命令发送失败。
     */
    void markFailed(Long recordId, String errorMessage);

    /**
     * 根据 commandId 标记命令执行成功。
     */
    boolean markSuccessByCommandId(String commandId, String replyPayload, LocalDateTime ackTime);

    /**
     * 根据 commandId 标记命令执行失败。
     */
    boolean markFailedByCommandId(String commandId, String replyPayload, String errorMessage, LocalDateTime ackTime);

    /**
     * 为后续超时扫描预留最小状态更新接口。
     */
    boolean markTimeout(String commandId, LocalDateTime timeoutTime);

    /**
     * 按设备编码查询命令台账。
     */
    PageResult<CommandRecordPageItemVO> pageByDevice(Long currentUserId,
                                                     String deviceCode,
                                                     String capabilityCode,
                                                     String status,
                                                     Long pageNum,
                                                     Long pageSize);
}
