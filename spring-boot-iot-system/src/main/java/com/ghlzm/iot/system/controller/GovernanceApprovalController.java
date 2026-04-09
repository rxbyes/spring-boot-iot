package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceApprovalDecisionDTO;
import com.ghlzm.iot.system.dto.GovernanceApprovalResubmitDTO;
import com.ghlzm.iot.system.service.GovernanceApprovalQueryService;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderDetailVO;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Governance approval read side controller.
 */
@RestController
@RequestMapping("/api/system/governance-approval")
public class GovernanceApprovalController {

    private final GovernanceApprovalQueryService governanceApprovalQueryService;
    private final GovernanceApprovalService governanceApprovalService;

    public GovernanceApprovalController(GovernanceApprovalQueryService governanceApprovalQueryService,
                                        GovernanceApprovalService governanceApprovalService) {
        this.governanceApprovalQueryService = governanceApprovalQueryService;
        this.governanceApprovalService = governanceApprovalService;
    }

    @GetMapping("/page")
    public R<PageResult<GovernanceApprovalOrderVO>> pageOrders(@RequestParam(required = false) String actionCode,
                                                               @RequestParam(required = false) String subjectType,
                                                               @RequestParam(required = false) Long subjectId,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Long operatorUserId,
                                                               @RequestParam(required = false) Long approverUserId,
                                                               @RequestParam(defaultValue = "1") Long pageNum,
                                                               @RequestParam(defaultValue = "10") Long pageSize,
                                                               Authentication authentication) {
        return R.ok(governanceApprovalQueryService.pageOrders(
                requireCurrentUserId(authentication),
                actionCode,
                subjectType,
                subjectId,
                status,
                operatorUserId,
                approverUserId,
                pageNum,
                pageSize
        ));
    }

    @GetMapping("/{orderId:[0-9]+}")
    public R<GovernanceApprovalOrderDetailVO> getOrderDetail(@PathVariable Long orderId,
                                                              Authentication authentication) {
        return R.ok(governanceApprovalQueryService.getOrderDetail(requireCurrentUserId(authentication), orderId));
    }

    @PostMapping("/{orderId:[0-9]+}/approve")
    public R<Void> approveOrder(@PathVariable Long orderId,
                                @RequestBody(required = false) GovernanceApprovalDecisionDTO dto,
                                Authentication authentication) {
        governanceApprovalService.approveOrder(orderId, requireCurrentUserId(authentication), commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{orderId:[0-9]+}/reject")
    public R<Void> rejectOrder(@PathVariable Long orderId,
                               @RequestBody(required = false) GovernanceApprovalDecisionDTO dto,
                               Authentication authentication) {
        governanceApprovalService.rejectOrder(orderId, requireCurrentUserId(authentication), commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{orderId:[0-9]+}/cancel")
    public R<Void> cancelOrder(@PathVariable Long orderId,
                               @RequestBody(required = false) GovernanceApprovalDecisionDTO dto,
                               Authentication authentication) {
        governanceApprovalService.cancelOrder(orderId, requireCurrentUserId(authentication), commentOf(dto));
        return R.ok();
    }

    @PostMapping("/{orderId:[0-9]+}/resubmit")
    public R<Void> resubmitOrder(@PathVariable Long orderId,
                                 @RequestBody @Valid GovernanceApprovalResubmitDTO dto,
                                 Authentication authentication) {
        governanceApprovalService.resubmitOrder(
                orderId,
                requireCurrentUserId(authentication),
                dto.getApproverUserId(),
                dto.getComment()
        );
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }

    private String commentOf(GovernanceApprovalDecisionDTO dto) {
        return dto == null ? null : dto.getComment();
    }
}
