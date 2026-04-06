package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/metric-catalogs")
    public R<PageResult<RiskMetricCatalogItemVO>> pageMetricCatalogs(@RequestParam(required = false) Long productId,
                                                                     @RequestParam(required = false) Long pageNum,
                                                                     @RequestParam(required = false) Long pageSize) {
        return R.ok(riskGovernanceService.pageMetricCatalogs(productId, pageNum, pageSize));
    }

    @GetMapping("/metric-catalogs/{id}")
    public R<RiskMetricCatalogItemVO> getMetricCatalog(@PathVariable Long id) {
        return R.ok(riskGovernanceService.getMetricCatalog(id));
    }

    @GetMapping("/coverage-overview")
    public R<RiskGovernanceCoverageOverviewVO> getCoverageOverview(@RequestParam Long productId) {
        return R.ok(riskGovernanceService.getCoverageOverview(productId));
    }

    @GetMapping("/dashboard-overview")
    public R<RiskGovernanceDashboardOverviewVO> getDashboardOverview() {
        return R.ok(riskGovernanceService.getDashboardOverview());
    }
}
