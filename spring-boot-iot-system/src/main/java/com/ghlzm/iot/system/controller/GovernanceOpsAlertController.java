package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceOpsAlertTransitionDTO;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceOpsAlertService;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertPageQuery;
import com.ghlzm.iot.system.vo.GovernanceOpsAlertVO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/ops-alerts")
public class GovernanceOpsAlertController {

    private final GovernanceOpsAlertService governanceOpsAlertService;
    private final GovernancePermissionGuard permissionGuard;

    public GovernanceOpsAlertController(GovernanceOpsAlertService governanceOpsAlertService,
                                        GovernancePermissionGuard permissionGuard) {
        this.governanceOpsAlertService = governanceOpsAlertService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public R<PageResult<GovernanceOpsAlertVO>> pageAlerts(GovernanceOpsAlertPageQuery query,
                                                          Authentication authentication) {
        return R.ok(governanceOpsAlertService.pageAlerts(query, requireCurrentUserId(authentication)));
    }

    @PostMapping("/{id:[0-9]+}/ack")
    public R<Void> ackAlert(@PathVariable Long id,
                            @RequestBody(required = false) GovernanceOpsAlertTransitionDTO dto,
                            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "治理运维告警确认",
                GovernancePermissionCodes.GOVERNANCE_OPS_ACK
        );
        governanceOpsAlertService.ack(id, currentUserId, commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{id:[0-9]+}/suppress")
    public R<Void> suppressAlert(@PathVariable Long id,
                                 @RequestBody(required = false) GovernanceOpsAlertTransitionDTO dto,
                                 Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "治理运维告警抑制",
                GovernancePermissionCodes.GOVERNANCE_OPS_SUPPRESS
        );
        governanceOpsAlertService.suppress(id, currentUserId, commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{id:[0-9]+}/close")
    public R<Void> closeAlert(@PathVariable Long id,
                              @RequestBody(required = false) GovernanceOpsAlertTransitionDTO dto,
                              Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "治理运维告警关闭",
                GovernancePermissionCodes.GOVERNANCE_OPS_CLOSE
        );
        governanceOpsAlertService.close(id, currentUserId, commentOf(dto));
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }

    private String commentOf(GovernanceOpsAlertTransitionDTO dto) {
        return dto == null ? null : dto.getComment();
    }
}
