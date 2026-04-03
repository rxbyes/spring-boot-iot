package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskPointPendingBindingQuery;
import com.ghlzm.iot.alarm.service.RiskPointPendingBindingService;
import com.ghlzm.iot.alarm.service.RiskPointPendingRecommendationService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingBindingItemVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
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
 * 风险点待治理只读接口。
 */
@RestController
@RequestMapping("/api/risk-point")
public class RiskPointPendingController {

    private final RiskPointPendingBindingService pendingBindingService;
    private final RiskPointPendingRecommendationService pendingRecommendationService;

    public RiskPointPendingController(RiskPointPendingBindingService pendingBindingService,
                                      RiskPointPendingRecommendationService pendingRecommendationService) {
        this.pendingBindingService = pendingBindingService;
        this.pendingRecommendationService = pendingRecommendationService;
    }

    @GetMapping("/pending-bindings")
    public R<PageResult<RiskPointPendingBindingItemVO>> pagePendingBindings(
            @RequestParam Long riskPointId,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String resolutionStatus,
            @RequestParam(required = false) String batchNo,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            Authentication authentication) {
        RiskPointPendingBindingQuery query = new RiskPointPendingBindingQuery();
        query.setRiskPointId(riskPointId);
        query.setDeviceCode(deviceCode);
        query.setResolutionStatus(resolutionStatus);
        query.setBatchNo(batchNo);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return R.ok(pendingBindingService.pagePendingBindings(query, requireCurrentUserId(authentication)));
    }

    @GetMapping("/pending-bindings/{pendingId}/candidates")
    public R<RiskPointPendingCandidateBundleVO> getCandidates(@PathVariable Long pendingId, Authentication authentication) {
        return R.ok(pendingRecommendationService.getCandidates(pendingId, requireCurrentUserId(authentication)));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
