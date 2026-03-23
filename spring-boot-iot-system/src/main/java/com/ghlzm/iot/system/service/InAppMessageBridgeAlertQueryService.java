package com.ghlzm.iot.system.service;

import java.util.Date;
import java.util.List;

/**
 * 站内消息桥接失败信号查询服务。
 */
public interface InAppMessageBridgeAlertQueryService {

    /**
     * 统计指定时间之后各渠道的桥接失败次数。
     */
    List<ChannelFailureCount> listFailedAttemptCountsSince(Date startTime);

    record ChannelFailureCount(String channelCode, String channelName, long failureCount) {
    }
}
