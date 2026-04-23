package com.ghlzm.iot.system.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.service.model.GovernanceReplayFeedbackCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceDecisionContextVO;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;

public interface GovernanceWorkItemService {

    void openOrRefresh(GovernanceWorkItemCommand command);

    Long openOrRefreshAndGetId(GovernanceWorkItemCommand command);

    void resolve(String workItemCode, String subjectType, Long subjectId, Long operatorUserId, String comment);

    PageResult<GovernanceWorkItemVO> pageWorkItems(GovernanceWorkItemPageQuery query, Long currentUserId);

    GovernanceDecisionContextVO getDecisionContext(Long workItemId, Long currentUserId);

    void ack(Long workItemId, Long currentUserId, String comment);

    void block(Long workItemId, Long currentUserId, String comment);

    void close(Long workItemId, Long currentUserId, String comment);

    void closeReplayWithFeedback(GovernanceReplayFeedbackCommand command, Long currentUserId);
}
