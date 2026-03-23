package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.mapper.InAppMessageBridgeAttemptLogMapper;
import com.ghlzm.iot.system.service.InAppMessageBridgeAlertQueryService;
import com.ghlzm.iot.system.vo.InAppMessageBridgeFailureCountVO;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 站内消息桥接失败信号查询实现。
 */
@Service
public class InAppMessageBridgeAlertQueryServiceImpl implements InAppMessageBridgeAlertQueryService {

    private final InAppMessageBridgeAttemptLogMapper inAppMessageBridgeAttemptLogMapper;
    private final SystemContentSchemaSupport systemContentSchemaSupport;

    public InAppMessageBridgeAlertQueryServiceImpl(InAppMessageBridgeAttemptLogMapper inAppMessageBridgeAttemptLogMapper,
                                                   SystemContentSchemaSupport systemContentSchemaSupport) {
        this.inAppMessageBridgeAttemptLogMapper = inAppMessageBridgeAttemptLogMapper;
        this.systemContentSchemaSupport = systemContentSchemaSupport;
    }

    @Override
    public List<ChannelFailureCount> listFailedAttemptCountsSince(Date startTime) {
        systemContentSchemaSupport.ensureInAppMessageBridgeAttemptLogReady();
        List<InAppMessageBridgeFailureCountVO> buckets =
                inAppMessageBridgeAttemptLogMapper.listFailedAttemptCountsByChannel(startTime);
        if (buckets == null || buckets.isEmpty()) {
            return List.of();
        }
        return buckets.stream()
                .map(bucket -> new ChannelFailureCount(
                        bucket.getChannelCode(),
                        bucket.getChannelName(),
                        bucket.getFailureCount() == null ? 0L : bucket.getFailureCount()
                ))
                .toList();
    }
}
