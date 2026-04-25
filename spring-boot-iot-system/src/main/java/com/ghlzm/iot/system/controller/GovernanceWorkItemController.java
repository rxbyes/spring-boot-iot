package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceWorkItemTransitionDTO;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.model.GovernanceReplayFeedbackCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceDecisionContextVO;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final GovernancePermissionGuard permissionGuard;

    public GovernanceWorkItemController(GovernanceWorkItemService governanceWorkItemService) {
        this(governanceWorkItemService, null);
    }

    @Autowired
    public GovernanceWorkItemController(GovernanceWorkItemService governanceWorkItemService,
                                        GovernancePermissionGuard permissionGuard) {
        this.governanceWorkItemService = governanceWorkItemService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public R<PageResult<GovernanceWorkItemVO>> pageWorkItems(GovernanceWorkItemPageQuery query,
                                                             Authentication authentication) {
        return R.ok(governanceWorkItemService.pageWorkItems(query, requireCurrentUserId(authentication)));
    }

    @GetMapping("/{id:[0-9]+}/decision-context")
    public R<GovernanceDecisionContextVO> getDecisionContext(@PathVariable Long id,
                                                             Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "治理任务决策说明",
                GovernancePermissionCodes.GOVERNANCE_TASK_DECISION_CONTEXT
        );
        return R.ok(governanceWorkItemService.getDecisionContext(id, currentUserId));
    }

    @PostMapping("/{id:[0-9]+}/ack")
    public R<Void> ackWorkItem(@PathVariable Long id,
                               @RequestBody(required = false) GovernanceWorkItemTransitionDTO dto,
                               Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "治理任务确认",
                GovernancePermissionCodes.GOVERNANCE_TASK_ACK
        );
        governanceWorkItemService.ack(id, currentUserId, commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{id:[0-9]+}/block")
    public R<Void> blockWorkItem(@PathVariable Long id,
                                 @RequestBody(required = false) GovernanceWorkItemTransitionDTO dto,
                                 Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "治理任务阻塞",
                GovernancePermissionCodes.GOVERNANCE_TASK_BLOCK
        );
        governanceWorkItemService.block(id, currentUserId, commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{id:[0-9]+}/close")
    public R<Void> closeWorkItem(@PathVariable Long id,
                                 @RequestBody(required = false) GovernanceWorkItemTransitionDTO dto,
                                 Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "治理任务关闭",
                GovernancePermissionCodes.GOVERNANCE_TASK_CLOSE
        );
        governanceWorkItemService.close(id, currentUserId, commentOf(dto));
        return R.ok();
    }

    @PostMapping("/replay-feedback")
    public R<Void> closeReplayWithFeedback(@RequestBody GovernanceWorkItemTransitionDTO dto,
                                           Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "治理复盘结论提交",
                GovernancePermissionCodes.GOVERNANCE_TASK_REPLAY_FEEDBACK,
                GovernancePermissionCodes.GOVERNANCE_OPS_REPLAY_FEEDBACK
        );
        governanceWorkItemService.closeReplayWithFeedback(replayFeedbackCommandOf(dto), currentUserId);
        return R.ok();
    }

    private void requirePermission(Long currentUserId, String actionName, String... permissionCodes) {
        if (permissionGuard != null) {
            permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCodes);
        }
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

    private GovernanceReplayFeedbackCommand replayFeedbackCommandOf(GovernanceWorkItemTransitionDTO dto) {
        if (dto == null) {
            return new GovernanceReplayFeedbackCommand(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
        return new GovernanceReplayFeedbackCommand(
                dto.getWorkItemId(),
                dto.getApprovalOrderId(),
                dto.getReleaseBatchId(),
                dto.getTraceId(),
                dto.getDeviceCode(),
                dto.getProductKey(),
                dto.getRecommendedDecision(),
                dto.getAdoptedDecision(),
                dto.getExecutionOutcome(),
                dto.getRootCauseCode(),
                dto.getOperatorSummary()
        );
    }
}
