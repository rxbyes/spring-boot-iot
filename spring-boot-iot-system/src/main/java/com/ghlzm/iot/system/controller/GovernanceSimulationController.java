package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.service.GovernanceApprovalQueryService;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.service.model.GovernanceSimulationResult;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Governance dry-run simulation endpoints.
 */
@RestController
@RequestMapping("/api/system/governance-simulation")
public class GovernanceSimulationController {

    private final GovernanceApprovalQueryService governanceApprovalQueryService;
    private final GovernanceApprovalService governanceApprovalService;

    public GovernanceSimulationController(GovernanceApprovalQueryService governanceApprovalQueryService,
                                          GovernanceApprovalService governanceApprovalService) {
        this.governanceApprovalQueryService = governanceApprovalQueryService;
        this.governanceApprovalService = governanceApprovalService;
    }

    @PostMapping("/approval/{orderId:[0-9]+}")
    public R<GovernanceSimulationResult> simulateApproval(@PathVariable Long orderId,
                                                          Authentication authentication) {
        governanceApprovalQueryService.getOrderDetail(requireCurrentUserId(authentication), orderId);
        return R.ok(governanceApprovalService.simulateOrder(orderId));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
