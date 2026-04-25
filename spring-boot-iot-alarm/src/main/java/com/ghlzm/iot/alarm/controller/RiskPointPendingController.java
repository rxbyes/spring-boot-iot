package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskPointPendingBindingQuery;
import com.ghlzm.iot.alarm.dto.RiskPointPendingIgnoreRequest;
import com.ghlzm.iot.alarm.dto.RiskPointPendingPromotionRequest;
import com.ghlzm.iot.alarm.service.RiskPointPendingBindingService;
import com.ghlzm.iot.alarm.service.RiskPointPendingPromotionService;
import com.ghlzm.iot.alarm.service.RiskPointPendingRecommendationService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingBindingItemVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风险点待治理接口。
 */
@RestController
@RequestMapping("/api/risk-point")
public class RiskPointPendingController {

    private final RiskPointPendingBindingService pendingBindingService;
    private final RiskPointPendingRecommendationService pendingRecommendationService;
    private final RiskPointPendingPromotionService pendingPromotionService;
    private final GovernancePermissionGuard permissionGuard;

    public RiskPointPendingController(RiskPointPendingBindingService pendingBindingService,
                                      RiskPointPendingRecommendationService pendingRecommendationService,
                                      RiskPointPendingPromotionService pendingPromotionService) {
        this(pendingBindingService, pendingRecommendationService, pendingPromotionService, null);
    }

    @Autowired
    public RiskPointPendingController(RiskPointPendingBindingService pendingBindingService,
                                      RiskPointPendingRecommendationService pendingRecommendationService,
                                      RiskPointPendingPromotionService pendingPromotionService,
                                      GovernancePermissionGuard permissionGuard) {
        this.pendingBindingService = pendingBindingService;
        this.pendingRecommendationService = pendingRecommendationService;
        this.pendingPromotionService = pendingPromotionService;
        this.permissionGuard = permissionGuard;
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

    @PostMapping("/pending-bindings/{pendingId}/promote")
    public R<GovernanceSubmissionResultVO> promote(@PathVariable Long pendingId,
                                                   @RequestBody RiskPointPendingPromotionRequest request,
                                                   Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "待治理转正", GovernancePermissionCodes.RISK_POINT_PENDING_PROMOTION_EXECUTE);
        return R.ok(pendingPromotionService.submitPromotion(pendingId, request, currentUserId));
    }

    @PostMapping("/pending-bindings/{pendingId}/ignore")
    public R<Void> ignore(@PathVariable Long pendingId,
                          @RequestBody RiskPointPendingIgnoreRequest request,
                          Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "忽略待治理绑定", GovernancePermissionCodes.RISK_POINT_PENDING_PROMOTION_EXECUTE);
        pendingPromotionService.ignore(pendingId, request, currentUserId);
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }

    private void requirePermission(Long currentUserId, String actionName, String permissionCode) {
        if (permissionGuard != null) {
            permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
        }
    }
}
