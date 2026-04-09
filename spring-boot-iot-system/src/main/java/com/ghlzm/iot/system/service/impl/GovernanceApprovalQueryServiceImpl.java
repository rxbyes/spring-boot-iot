package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.entity.GovernanceApprovalTransition;
import com.ghlzm.iot.system.mapper.GovernanceApprovalOrderMapper;
import com.ghlzm.iot.system.mapper.GovernanceApprovalTransitionMapper;
import com.ghlzm.iot.system.service.GovernanceApprovalQueryService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderDetailVO;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderVO;
import com.ghlzm.iot.system.vo.GovernanceApprovalTransitionVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Governance approval read side implementation.
 */
@Service
public class GovernanceApprovalQueryServiceImpl implements GovernanceApprovalQueryService {

    private final GovernanceApprovalOrderMapper orderMapper;
    private final GovernanceApprovalTransitionMapper transitionMapper;
    private final PermissionService permissionService;

    public GovernanceApprovalQueryServiceImpl(GovernanceApprovalOrderMapper orderMapper,
                                              GovernanceApprovalTransitionMapper transitionMapper,
                                              PermissionService permissionService) {
        this.orderMapper = orderMapper;
        this.transitionMapper = transitionMapper;
        this.permissionService = permissionService;
    }

    @Override
    public PageResult<GovernanceApprovalOrderVO> pageOrders(Long currentUserId,
                                                            String actionCode,
                                                            String subjectType,
                                                            Long subjectId,
                                                            String status,
                                                            Long operatorUserId,
                                                            Long approverUserId,
                                                            Long pageNum,
                                                            Long pageSize) {
        Long tenantId = resolveTenantId(currentUserId);
        Page<GovernanceApprovalOrder> page = PageQueryUtils.buildPage(pageNum, pageSize);
        LambdaQueryWrapper<GovernanceApprovalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, GovernanceApprovalOrder::getTenantId, tenantId)
                .like(StringUtils.hasText(actionCode), GovernanceApprovalOrder::getActionCode, trim(actionCode))
                .eq(StringUtils.hasText(subjectType), GovernanceApprovalOrder::getSubjectType, trim(subjectType))
                .eq(subjectId != null, GovernanceApprovalOrder::getSubjectId, subjectId)
                .eq(StringUtils.hasText(status), GovernanceApprovalOrder::getStatus, normalizeStatus(status))
                .eq(operatorUserId != null, GovernanceApprovalOrder::getOperatorUserId, operatorUserId)
                .eq(approverUserId != null, GovernanceApprovalOrder::getApproverUserId, approverUserId)
                .orderByDesc(GovernanceApprovalOrder::getCreateTime)
                .orderByDesc(GovernanceApprovalOrder::getId);
        Page<GovernanceApprovalOrder> result = orderMapper.selectPage(page, wrapper);
        List<GovernanceApprovalOrderVO> records = result.getRecords().stream()
                .map(this::toOrderVO)
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public GovernanceApprovalOrderDetailVO getOrderDetail(Long currentUserId, Long orderId) {
        if (orderId == null) {
            throw new BizException("审批主单不存在: " + orderId);
        }
        GovernanceApprovalOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("审批主单不存在: " + orderId);
        }
        ensureTenantAccessible(currentUserId, order.getTenantId());
        List<GovernanceApprovalTransitionVO> transitions = transitionMapper.selectList(
                        new LambdaQueryWrapper<GovernanceApprovalTransition>()
                                .eq(GovernanceApprovalTransition::getOrderId, orderId)
                                .orderByAsc(GovernanceApprovalTransition::getCreateTime)
                                .orderByAsc(GovernanceApprovalTransition::getId)
                ).stream()
                .map(this::toTransitionVO)
                .toList();
        GovernanceApprovalOrderDetailVO detail = new GovernanceApprovalOrderDetailVO();
        detail.setOrder(toOrderVO(order));
        detail.setTransitions(transitions);
        return detail;
    }

    private GovernanceApprovalOrderVO toOrderVO(GovernanceApprovalOrder order) {
        GovernanceApprovalOrderVO vo = new GovernanceApprovalOrderVO();
        vo.setId(order.getId());
        vo.setActionCode(order.getActionCode());
        vo.setActionName(order.getActionName());
        vo.setSubjectType(order.getSubjectType());
        vo.setSubjectId(order.getSubjectId());
        vo.setStatus(order.getStatus());
        vo.setOperatorUserId(order.getOperatorUserId());
        vo.setApproverUserId(order.getApproverUserId());
        vo.setPayloadJson(order.getPayloadJson());
        vo.setApprovalComment(order.getApprovalComment());
        vo.setApprovedTime(order.getApprovedTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    private GovernanceApprovalTransitionVO toTransitionVO(GovernanceApprovalTransition transition) {
        GovernanceApprovalTransitionVO vo = new GovernanceApprovalTransitionVO();
        vo.setId(transition.getId());
        vo.setFromStatus(transition.getFromStatus());
        vo.setToStatus(transition.getToStatus());
        vo.setActorUserId(transition.getActorUserId());
        vo.setTransitionComment(transition.getTransitionComment());
        vo.setCreateTime(transition.getCreateTime());
        return vo;
    }

    private Long resolveTenantId(Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return null;
        }
        return context.tenantId();
    }

    private void ensureTenantAccessible(Long currentUserId, Long tenantId) {
        if (currentUserId == null) {
            return;
        }
        DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return;
        }
        if (context.tenantId() != null && !context.tenantId().equals(tenantId)) {
            throw new BizException("审批主单不存在或无权访问");
        }
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    }

    private String trim(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
