package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.service.RiskGovernanceOpsService;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceMissingPolicyProductMetricSummaryVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceOpsAlertItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReplayVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReleaseBatchDiffVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
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
    private final RiskGovernanceOpsService riskGovernanceOpsService;

    public RiskGovernanceController(RiskGovernanceService riskGovernanceService,
                                    RiskGovernanceOpsService riskGovernanceOpsService) {
        this.riskGovernanceService = riskGovernanceService;
        this.riskGovernanceOpsService = riskGovernanceOpsService;
    }

    @GetMapping("/missing-bindings")
    public R<PageResult<RiskGovernanceGapItemVO>> listMissingBindings(RiskGovernanceGapQuery query) {
        return R.ok(riskGovernanceService.listMissingBindings(query));
    }

    @GetMapping("/missing-policies")
    public R<PageResult<RiskGovernanceGapItemVO>> listMissingPolicies(RiskGovernanceGapQuery query) {
        return R.ok(riskGovernanceService.listMissingPolicies(query));
    }

    @GetMapping("/missing-policies/product-metric-summaries")
    public R<PageResult<RiskGovernanceMissingPolicyProductMetricSummaryVO>> pageMissingPolicyProductMetricSummaries(
            RiskGovernanceGapQuery query) {
        return R.ok(riskGovernanceService.pageMissingPolicyProductMetricSummaries(query));
    }

    @GetMapping("/metric-catalogs")
    public R<PageResult<RiskMetricCatalogItemVO>> pageMetricCatalogs(@RequestParam(required = false) Long productId,
                                                                     @RequestParam(required = false) Long releaseBatchId,
                                                                     @RequestParam(required = false) Long pageNum,
                                                                     @RequestParam(required = false) Long pageSize) {
        return R.ok(riskGovernanceService.pageMetricCatalogs(productId, releaseBatchId, pageNum, pageSize));
    }

    @GetMapping("/release-batch-diff")
    public R<RiskGovernanceReleaseBatchDiffVO> getReleaseBatchDiff(@RequestParam Long baselineBatchId,
                                                                   @RequestParam Long targetBatchId) {
        return R.ok(riskGovernanceService.compareReleaseBatches(baselineBatchId, targetBatchId));
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

    @GetMapping("/ops-alerts")
    public R<PageResult<RiskGovernanceOpsAlertItemVO>> pageOpsAlerts(@RequestParam(required = false) Long productId,
                                                                      @RequestParam(required = false) String alertType,
                                                                      @RequestParam(required = false) Long pageNum,
                                                                      @RequestParam(required = false) Long pageSize) {
        return R.ok(riskGovernanceOpsService.pageOpsAlerts(productId, alertType, pageNum, pageSize));
    }

    @GetMapping("/replay")
    public R<RiskGovernanceReplayVO> replay(@RequestParam(required = false) String traceId,
                                            @RequestParam(required = false) String deviceCode,
                                            @RequestParam(required = false) String productKey,
                                            @RequestParam(required = false) Long releaseBatchId,
                                            Authentication authentication) {
        return R.ok(riskGovernanceOpsService.replay(
                requireCurrentUserId(authentication),
                traceId,
                deviceCode,
                productKey,
                releaseBatchId
        ));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
