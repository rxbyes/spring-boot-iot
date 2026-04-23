package com.ghlzm.iot.system.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceApprovalPolicyResolverImplTest {

    @Mock
    private GovernanceApprovalPolicyMapper policyMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private RoleMapper roleMapper;

    private GovernanceApprovalPolicyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new GovernanceApprovalPolicyResolverImpl(policyMapper, userMapper, userRoleMapper, roleMapper);
    }

    @Test
    void resolveApproverUserIdShouldPreferTenantPolicyAndFallbackToGlobal() {
        GovernanceApprovalPolicy tenantPolicy = policy(91001L, 1L, "TENANT", "PRODUCT_CONTRACT_RELEASE_APPLY", 30003L, 1);
        GovernanceApprovalPolicy globalPolicy = policy(91002L, 0L, "GLOBAL", "PRODUCT_CONTRACT_RELEASE_APPLY", 20002L, 1);
        User operator = user(10001L, 1L, 7101L, 1);
        User approver = user(30003L, 1L, 7101L, 1);
        Role superAdmin = role(92000005L, 1L, "SUPER_ADMIN", 1);

        when(userMapper.selectById(10001L)).thenReturn(operator);
        when(policyMapper.selectList(any())).thenReturn(List.of(tenantPolicy, globalPolicy));
        when(userMapper.selectById(30003L)).thenReturn(approver);
        when(userRoleMapper.selectRoleIdsByUserId(30003L)).thenReturn(List.of(92000005L));
        when(roleMapper.selectBatchIds(List.of(92000005L))).thenReturn(List.of(superAdmin));

        Long approverUserId = resolver.resolveApproverUserId("PRODUCT_CONTRACT_RELEASE_APPLY", 10001L);

        assertEquals(30003L, approverUserId);
    }

    @Test
    void resolveApproverUserIdShouldRejectOperatorAsFixedApprover() {
        GovernanceApprovalPolicy globalPolicy = policy(91002L, 0L, "GLOBAL", "PRODUCT_CONTRACT_RELEASE_APPLY", 10001L, 1);
        User operator = user(10001L, 1L, 7101L, 1);

        when(userMapper.selectById(10001L)).thenReturn(operator);
        when(policyMapper.selectList(any())).thenReturn(List.of(globalPolicy));

        assertThrows(BizException.class, () -> resolver.resolveApproverUserId("PRODUCT_CONTRACT_RELEASE_APPLY", 10001L));
    }

    private GovernanceApprovalPolicy policy(Long id,
                                            Long tenantId,
                                            String scopeType,
                                            String actionCode,
                                            Long approverUserId,
                                            Integer enabled) {
        GovernanceApprovalPolicy policy = new GovernanceApprovalPolicy();
        policy.setId(id);
        policy.setTenantId(tenantId);
        policy.setScopeType(scopeType);
        policy.setActionCode(actionCode);
        policy.setApproverMode("FIXED_USER");
        policy.setApproverUserId(approverUserId);
        policy.setEnabled(enabled);
        policy.setDeleted(0);
        return policy;
    }

    private User user(Long id, Long tenantId, Long orgId, Integer status) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setOrgId(orgId);
        user.setStatus(status);
        user.setDeleted(0);
        return user;
    }

    private Role role(Long id, Long tenantId, String roleCode, Integer status) {
        Role role = new Role();
        role.setId(id);
        role.setTenantId(tenantId);
        role.setRoleCode(roleCode);
        role.setStatus(status);
        role.setDeleted(0);
        return role;
    }
}
