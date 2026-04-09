package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceWorkItemTransitionDTO;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/work-items")
public class GovernanceWorkItemController {

    private final GovernanceWorkItemService governanceWorkItemService;

    public GovernanceWorkItemController(GovernanceWorkItemService governanceWorkItemService) {
        this.governanceWorkItemService = governanceWorkItemService;
    }

    @GetMapping
    public R<PageResult<GovernanceWorkItemVO>> pageWorkItems(GovernanceWorkItemPageQuery query,
                                                             Authentication authentication) {
        return R.ok(governanceWorkItemService.pageWorkItems(query, requireCurrentUserId(authentication)));
    }

    @PostMapping("/{id:[0-9]+}/ack")
    public R<Void> ackWorkItem(@PathVariable Long id,
                               @RequestBody(required = false) GovernanceWorkItemTransitionDTO dto,
                               Authentication authentication) {
        governanceWorkItemService.ack(id, requireCurrentUserId(authentication), commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{id:[0-9]+}/block")
    public R<Void> blockWorkItem(@PathVariable Long id,
                                 @RequestBody(required = false) GovernanceWorkItemTransitionDTO dto,
                                 Authentication authentication) {
        governanceWorkItemService.block(id, requireCurrentUserId(authentication), commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{id:[0-9]+}/close")
    public R<Void> closeWorkItem(@PathVariable Long id,
                                 @RequestBody(required = false) GovernanceWorkItemTransitionDTO dto,
                                 Authentication authentication) {
        governanceWorkItemService.close(id, requireCurrentUserId(authentication), commentOf(dto));
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }

    private String commentOf(GovernanceWorkItemTransitionDTO dto) {
        return dto == null ? null : dto.getComment();
    }
}
