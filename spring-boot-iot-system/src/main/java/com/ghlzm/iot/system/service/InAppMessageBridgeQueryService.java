package com.ghlzm.iot.system.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.vo.InAppMessageBridgeAttemptVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeLogVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeStatsVO;

import java.util.Date;
import java.util.List;

public interface InAppMessageBridgeQueryService {

    default InAppMessageBridgeStatsVO getBridgeStats(Date startTime,
                                                     Date endTime,
                                                     String messageType,
                                                     String sourceType,
                                                     String priority,
                                                     String channelCode,
                                                     Integer bridgeStatus) {
        return getBridgeStats(null, startTime, endTime, messageType, sourceType, priority, channelCode, bridgeStatus);
    }

    InAppMessageBridgeStatsVO getBridgeStats(Long currentUserId,
                                             Date startTime,
                                             Date endTime,
                                             String messageType,
                                             String sourceType,
                                             String priority,
                                             String channelCode,
                                             Integer bridgeStatus);

    default PageResult<InAppMessageBridgeLogVO> pageBridgeLogs(Date startTime,
                                                               Date endTime,
                                                               String messageType,
                                                               String sourceType,
                                                               String priority,
                                                               String channelCode,
                                                               Integer bridgeStatus,
                                                               Long pageNum,
                                                               Long pageSize) {
        return pageBridgeLogs(null, startTime, endTime, messageType, sourceType, priority, channelCode, bridgeStatus, pageNum, pageSize);
    }

    PageResult<InAppMessageBridgeLogVO> pageBridgeLogs(Long currentUserId,
                                                       Date startTime,
                                                       Date endTime,
                                                       String messageType,
                                                       String sourceType,
                                                       String priority,
                                                       String channelCode,
                                                       Integer bridgeStatus,
                                                       Long pageNum,
                                                       Long pageSize);

    default List<InAppMessageBridgeAttemptVO> listBridgeAttempts(Long bridgeLogId) {
        return listBridgeAttempts(null, bridgeLogId);
    }

    List<InAppMessageBridgeAttemptVO> listBridgeAttempts(Long currentUserId, Long bridgeLogId);
}
