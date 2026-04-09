package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceOpsAlertTransitionDTO;
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

    public GovernanceOpsAlertController(GovernanceOpsAlertService governanceOpsAlertService) {
        this.governanceOpsAlertService = governanceOpsAlertService;
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
        governanceOpsAlertService.ack(id, requireCurrentUserId(authentication), commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{id:[0-9]+}/suppress")
    public R<Void> suppressAlert(@PathVariable Long id,
                                 @RequestBody(required = false) GovernanceOpsAlertTransitionDTO dto,
                                 Authentication authentication) {
        governanceOpsAlertService.suppress(id, requireCurrentUserId(authentication), commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{id:[0-9]+}/close")
    public R<Void> closeAlert(@PathVariable Long id,
                              @RequestBody(required = false) GovernanceOpsAlertTransitionDTO dto,
                              Authentication authentication) {
        governanceOpsAlertService.close(id, requireCurrentUserId(authentication), commentOf(dto));
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
