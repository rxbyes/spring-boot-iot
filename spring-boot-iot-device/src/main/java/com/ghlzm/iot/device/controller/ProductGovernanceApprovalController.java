package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.governance.ProductContractGovernanceApprovalPayloads;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalQueryService;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product governance approval helper controller.
 */
@RestController
public class ProductGovernanceApprovalController {

    private final GovernanceApprovalQueryService governanceApprovalQueryService;
    private final GovernanceApprovalService governanceApprovalService;
    private final GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver;
    private final GovernancePermissionGuard permissionGuard;

    public ProductGovernanceApprovalController(GovernanceApprovalQueryService governanceApprovalQueryService,
                                               GovernanceApprovalService governanceApprovalService,
                                               GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver) {
        this(governanceApprovalQueryService, governanceApprovalService, governanceApprovalPolicyResolver, null);
    }

    @Autowired
    public ProductGovernanceApprovalController(GovernanceApprovalQueryService governanceApprovalQueryService,
                                               GovernanceApprovalService governanceApprovalService,
                                               GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver,
                                               GovernancePermissionGuard permissionGuard) {
        this.governanceApprovalQueryService = governanceApprovalQueryService;
        this.governanceApprovalService = governanceApprovalService;
        this.governanceApprovalPolicyResolver = governanceApprovalPolicyResolver;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping("/api/device/product/governance-approval/{orderId}/resubmit")
    public R<Void> resubmitOrder(@PathVariable Long orderId, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        GovernanceApprovalOrderDetailVO detail = governanceApprovalQueryService.getOrderDetail(currentUserId, orderId);
        if (detail == null || detail.getOrder() == null) {
            throw new BizException("审批主单不存在: " + orderId);
        }
        String actionCode = detail.getOrder().getActionCode();
        if (!ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY.equals(actionCode)
                && !ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_ROLLBACK.equals(actionCode)) {
            throw new BizException("当前审批单不支持固定复核人自动重提");
        }
        requirePermission(
                currentUserId,
                "产品合同原单重提",
                ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_ROLLBACK.equals(actionCode)
                        ? GovernancePermissionCodes.PRODUCT_CONTRACT_ROLLBACK
                        : GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE
        );
        Long approverUserId = governanceApprovalPolicyResolver.resolveApproverUserId(actionCode, currentUserId);
        governanceApprovalService.resubmitOrder(orderId, currentUserId, approverUserId, null);
        return R.ok();
    }

    private void requirePermission(Long currentUserId, String actionName, String permissionCode) {
        if (permissionGuard != null) {
            permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
        }
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
