package com.ghlzm.iot.system.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderDetailVO;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderVO;

/**
 * Governance approval read side query service.
 */
public interface GovernanceApprovalQueryService {

    PageResult<GovernanceApprovalOrderVO> pageOrders(Long currentUserId,
                                                     String actionCode,
                                                     String subjectType,
                                                     Long subjectId,
                                                     String status,
                                                     Long operatorUserId,
                                                     Long approverUserId,
                                                     Long pageNum,
                                                     Long pageSize);

    GovernanceApprovalOrderDetailVO getOrderDetail(Long currentUserId, Long orderId);
}
