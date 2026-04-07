package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product model controller.
 */
@RestController
public class ProductModelController {

    private static final String ACTION_PRODUCT_CONTRACT_RELEASE_APPLY = "PRODUCT_CONTRACT_RELEASE_APPLY";

    private final ProductModelService productModelService;
    private final GovernancePermissionGuard permissionGuard;
    private final GovernanceApprovalService governanceApprovalService;

    public ProductModelController(ProductModelService productModelService,
                                  GovernancePermissionGuard permissionGuard,
                                  GovernanceApprovalService governanceApprovalService) {
        this.productModelService = productModelService;
        this.permissionGuard = permissionGuard;
        this.governanceApprovalService = governanceApprovalService;
    }

    @GetMapping("/api/device/product/{productId}/models")
    public R<List<ProductModelVO>> list(@PathVariable Long productId) {
        return R.ok(productModelService.listModels(productId));
    }

    @PostMapping("/api/device/product/{productId}/models")
    public R<ProductModelVO> add(@PathVariable Long productId,
                                 @RequestBody @Valid ProductModelUpsertDTO dto,
                                 Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "产品契约维护",
                GovernancePermissionCodes.NORMATIVE_LIBRARY_WRITE
        );
        return R.ok(productModelService.createModel(productId, dto));
    }

    @PostMapping("/api/device/product/{productId}/model-governance/compare")
    public R<ProductModelGovernanceCompareVO> compareGovernance(@PathVariable Long productId,
                                                                @RequestBody ProductModelGovernanceCompareDTO dto,
                                                                Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "产品契约治理",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        return R.ok(productModelService.compareGovernance(productId, dto));
    }

    @PostMapping("/api/device/product/{productId}/model-governance/apply")
    public R<ProductModelGovernanceApplyResultVO> applyGovernance(@PathVariable Long productId,
                                                                   @RequestBody ProductModelGovernanceApplyDTO dto,
                                                                   @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                                                                   Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "产品契约发布",
                GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE,
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        );
        permissionGuard.requireAnyPermission(
                currentUserId,
                "风险指标标注",
                GovernancePermissionCodes.RISK_METRIC_CATALOG_TAG
        );
        permissionGuard.requireAnyPermission(
                approverUserId,
                "风险指标标注复核",
                GovernancePermissionCodes.RISK_METRIC_CATALOG_APPROVE,
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        );
        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(productId, dto, currentUserId);
        Long approvalOrderId = governanceApprovalService.recordApprovedAction(new GovernanceApprovalActionCommand(
                ACTION_PRODUCT_CONTRACT_RELEASE_APPLY,
                "product contract release apply",
                "PRODUCT",
                productId,
                currentUserId,
                approverUserId,
                buildApplyPayload(productId, result),
                null
        ));
        result.setApprovalOrderId(approvalOrderId);
        return R.ok(result);
    }

    @PutMapping("/api/device/product/{productId}/models/{modelId}")
    public R<ProductModelVO> update(@PathVariable Long productId,
                                    @PathVariable Long modelId,
                                    @RequestBody @Valid ProductModelUpsertDTO dto,
                                    Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "产品契约维护",
                GovernancePermissionCodes.NORMATIVE_LIBRARY_WRITE
        );
        return R.ok(productModelService.updateModel(productId, modelId, dto));
    }

    @DeleteMapping("/api/device/product/{productId}/models/{modelId}")
    public R<Void> delete(@PathVariable Long productId,
                          @PathVariable Long modelId,
                          Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "产品契约维护",
                GovernancePermissionCodes.NORMATIVE_LIBRARY_WRITE
        );
        productModelService.deleteModel(productId, modelId);
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }

    private String buildApplyPayload(Long productId, ProductModelGovernanceApplyResultVO result) {
        Long releaseBatchId = result == null ? null : result.getReleaseBatchId();
        return "{\"productId\":" + productId + ",\"releaseBatchId\":" + releaseBatchId + "}";
    }
}
