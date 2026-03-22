package com.ghlzm.iot.system.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.vo.InAppMessageBridgeAttemptVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeLogVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeStatsVO;

import java.util.Date;
import java.util.List;

public interface InAppMessageBridgeQueryService {

    InAppMessageBridgeStatsVO getBridgeStats(Date startTime,
                                            Date endTime,
                                            String messageType,
                                            String sourceType,
                                            String priority,
                                            String channelCode,
                                            Integer bridgeStatus);

    PageResult<InAppMessageBridgeLogVO> pageBridgeLogs(Date startTime,
                                                       Date endTime,
                                                       String messageType,
                                                       String sourceType,
                                                       String priority,
                                                       String channelCode,
                                                       Integer bridgeStatus,
                                                       Long pageNum,
                                                       Long pageSize);

    List<InAppMessageBridgeAttemptVO> listBridgeAttempts(Long bridgeLogId);
}
