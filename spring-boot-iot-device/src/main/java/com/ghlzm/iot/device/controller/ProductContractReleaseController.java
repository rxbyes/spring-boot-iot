package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import com.ghlzm.iot.device.vo.ProductContractReleaseRollbackResultVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product contract release batch controller.
 */
@RestController
public class ProductContractReleaseController {

    private final ProductContractReleaseService productContractReleaseService;
    private final GovernancePermissionGuard permissionGuard;

    public ProductContractReleaseController(ProductContractReleaseService productContractReleaseService,
                                            GovernancePermissionGuard permissionGuard) {
        this.productContractReleaseService = productContractReleaseService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/api/device/product/{productId}/contract-release-batches")
    public R<PageResult<ProductContractReleaseBatchVO>> pageBatches(@PathVariable Long productId,
                                                                    @RequestParam(required = false) Long pageNum,
                                                                    @RequestParam(required = false) Long pageSize) {
        return R.ok(productContractReleaseService.pageBatches(productId, pageNum, pageSize));
    }

    @GetMapping("/api/device/product/contract-release-batches/{batchId}")
    public R<ProductContractReleaseBatchVO> getBatch(@PathVariable Long batchId) {
        return R.ok(productContractReleaseService.getBatch(batchId));
    }

    @PostMapping("/api/device/product/contract-release-batches/{batchId}/rollback")
    public R<ProductContractReleaseRollbackResultVO> rollbackBatch(@PathVariable Long batchId,
                                                                   @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                                                                   Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "契约发布回滚",
                GovernancePermissionCodes.PRODUCT_CONTRACT_ROLLBACK,
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        );
        return R.ok(productContractReleaseService.rollbackLatestBatch(batchId, currentUserId));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
