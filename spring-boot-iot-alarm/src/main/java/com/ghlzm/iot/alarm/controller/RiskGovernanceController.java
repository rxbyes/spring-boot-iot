package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风险治理缺口接口。
 */
@RestController
@RequestMapping("/api/risk-governance")
public class RiskGovernanceController {

    private final RiskGovernanceService riskGovernanceService;

    public RiskGovernanceController(RiskGovernanceService riskGovernanceService) {
        this.riskGovernanceService = riskGovernanceService;
    }

    @GetMapping("/missing-bindings")
    public R<PageResult<RiskGovernanceGapItemVO>> listMissingBindings(RiskGovernanceGapQuery query) {
        return R.ok(riskGovernanceService.listMissingBindings(query));
    }

    @GetMapping("/missing-policies")
    public R<PageResult<RiskGovernanceGapItemVO>> listMissingPolicies(RiskGovernanceGapQuery query) {
        return R.ok(riskGovernanceService.listMissingPolicies(query));
    }
}
