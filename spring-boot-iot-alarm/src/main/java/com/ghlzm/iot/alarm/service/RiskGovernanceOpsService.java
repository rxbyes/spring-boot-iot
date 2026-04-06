package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.vo.RiskGovernanceOpsAlertItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReplayVO;
import com.ghlzm.iot.common.response.PageResult;

/**
 * 风险治理运维侧能力服务。
 */
public interface RiskGovernanceOpsService {

    PageResult<RiskGovernanceOpsAlertItemVO> pageOpsAlerts(Long productId,
                                                           String alertType,
                                                           Long pageNum,
                                                           Long pageSize);

    RiskGovernanceReplayVO replay(Long currentUserId,
                                  String traceId,
                                  String deviceCode,
                                  String productKey);
}
