package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.common.response.PageResult;

/**
 * 风险治理缺口服务。
 */
public interface RiskGovernanceService {

    PageResult<RiskGovernanceGapItemVO> listMissingBindings(RiskGovernanceGapQuery query);

    PageResult<RiskGovernanceGapItemVO> listMissingPolicies(RiskGovernanceGapQuery query);
}
