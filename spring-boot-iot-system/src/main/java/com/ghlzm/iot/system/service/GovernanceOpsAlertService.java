package com.ghlzm.iot.system.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertCommand;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertPageQuery;
import com.ghlzm.iot.system.vo.GovernanceOpsAlertVO;

public interface GovernanceOpsAlertService {

    void raiseOrRefresh(GovernanceOpsAlertCommand command);

    void recover(String alertType, String alertCode, Long operatorUserId, String comment);

    PageResult<GovernanceOpsAlertVO> pageAlerts(GovernanceOpsAlertPageQuery query, Long currentUserId);

    void ack(Long alertId, Long currentUserId, String comment);

    void suppress(Long alertId, Long currentUserId, String comment);

    void close(Long alertId, Long currentUserId, String comment);
}
