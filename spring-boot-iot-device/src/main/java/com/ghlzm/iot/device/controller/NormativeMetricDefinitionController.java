package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.NormativeMetricDefinitionImportDTO;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.vo.NormativeMetricDefinitionImportResultVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 规范字段库治理控制器。
 */
@RestController
public class NormativeMetricDefinitionController {

    private final NormativeMetricDefinitionService service;
    private final GovernancePermissionGuard permissionGuard;

    public NormativeMetricDefinitionController(NormativeMetricDefinitionService service,
                                               GovernancePermissionGuard permissionGuard) {
        this.service = service;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping("/api/device/normative-metrics/import/preview")
    public R<NormativeMetricDefinitionImportResultVO> previewImport(
            @RequestBody @Valid NormativeMetricDefinitionImportDTO dto,
            Authentication authentication) {
        requireGovernancePermission(authentication, "规范字段库导入预检");
        return R.ok(service.previewImport(dto));
    }

    @PostMapping("/api/device/normative-metrics/import/apply")
    public R<NormativeMetricDefinitionImportResultVO> applyImport(
            @RequestBody @Valid NormativeMetricDefinitionImportDTO dto,
            Authentication authentication) {
        requireGovernancePermission(authentication, "规范字段库导入落库");
        return R.ok(service.applyImport(dto));
    }

    private Long requireGovernancePermission(Authentication authentication, String actionName) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        permissionGuard.requireAnyPermission(
                principal.userId(),
                actionName,
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        return principal.userId();
    }
}
