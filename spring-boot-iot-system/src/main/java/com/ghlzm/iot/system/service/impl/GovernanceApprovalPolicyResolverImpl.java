package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.GovernanceApprovalPolicy;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.GovernanceApprovalPolicyMapper;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.mapper.UserRoleMapper;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Governance approval policy resolver implementation.
 */
@Service
public class GovernanceApprovalPolicyResolverImpl implements GovernanceApprovalPolicyResolver {

    private static final String SCOPE_TENANT = "TENANT";
    private static final String SCOPE_GLOBAL = "GLOBAL";
    private static final String APPROVER_MODE_FIXED_USER = "FIXED_USER";
    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";

    private final GovernanceApprovalPolicyMapper policyMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public GovernanceApprovalPolicyResolverImpl(GovernanceApprovalPolicyMapper policyMapper,
                                                UserMapper userMapper,
                                                UserRoleMapper userRoleMapper,
                                                RoleMapper roleMapper) {
        this.policyMapper = policyMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public Long resolveApproverUserId(String actionCode, Long operatorUserId) {
        String normalizedActionCode = normalizeRequired(actionCode, "审批动作编码不能为空");
        User operator = requireActiveUser(operatorUserId, "审批执行人无效");
        GovernanceApprovalPolicy policy = loadPolicy(normalizedActionCode, operator.getTenantId());
        if (!APPROVER_MODE_FIXED_USER.equalsIgnoreCase(normalizeRequired(policy.getApproverMode(), "固定复核策略无效"))) {
            throw new BizException("当前动作未配置固定复核策略");
        }
        User approver = requireActiveUser(policy.getApproverUserId(), "固定复核人不存在或已停用");
        if (operatorUserId.equals(approver.getId())) {
            throw new BizException("当前执行人不能同时作为固定复核人");
        }
        ensureSuperAdmin(approver.getId());
        return approver.getId();
    }

    private GovernanceApprovalPolicy loadPolicy(String actionCode, Long tenantId) {
        List<GovernanceApprovalPolicy> policies = policyMapper.selectList(new LambdaQueryWrapper<GovernanceApprovalPolicy>()
                .eq(GovernanceApprovalPolicy::getEnabled, 1)
                .eq(GovernanceApprovalPolicy::getDeleted, 0)
                .eq(GovernanceApprovalPolicy::getActionCode, actionCode)
                .orderByAsc(GovernanceApprovalPolicy::getTenantId)
                .orderByAsc(GovernanceApprovalPolicy::getId));
        GovernanceApprovalPolicy tenantPolicy = null;
        GovernanceApprovalPolicy globalPolicy = null;
        for (GovernanceApprovalPolicy policy : policies) {
            if (policy == null) {
                continue;
            }
            String scopeType = normalize(policy.getScopeType());
            if (SCOPE_TENANT.equals(scopeType) && tenantId != null && tenantId.equals(policy.getTenantId()) && tenantPolicy == null) {
                tenantPolicy = policy;
            } else if (SCOPE_GLOBAL.equals(scopeType) && globalPolicy == null) {
                globalPolicy = policy;
            }
        }
        if (tenantPolicy != null) {
            return tenantPolicy;
        }
        if (globalPolicy != null) {
            return globalPolicy;
        }
        throw new BizException("当前动作未配置固定复核策略");
    }

    private User requireActiveUser(Long userId, String message) {
        if (userId == null || userId <= 0) {
            throw new BizException(message);
        }
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(message);
        }
        return user;
    }

    private void ensureSuperAdmin(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BizException("固定复核人缺少系统级审批权限");
        }
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        boolean matched = roles != null && roles.stream()
                .filter(role -> role != null && !Integer.valueOf(1).equals(role.getDeleted()) && Integer.valueOf(1).equals(role.getStatus()))
                .map(Role::getRoleCode)
                .anyMatch(roleCode -> SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(normalize(roleCode)));
        if (!matched) {
            throw new BizException("固定复核人缺少系统级审批权限");
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(message);
        }
        return normalized;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}
