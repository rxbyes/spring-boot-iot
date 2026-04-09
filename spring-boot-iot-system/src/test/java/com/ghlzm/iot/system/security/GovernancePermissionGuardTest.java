package com.ghlzm.iot.system.security;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernancePermissionGuardTest {

    @Mock
    private PermissionService permissionService;

    @Test
    void requireAnyPermissionShouldPassWhenUserHasGrantedCode() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);
        UserAuthContextVO context = new UserAuthContextVO();
        context.setPermissions(List.of(GovernancePermissionCodes.RULE_DEFINITION_EDIT));
        when(permissionService.getUserAuthContext(1001L)).thenReturn(context);

        assertDoesNotThrow(() -> guard.requireAnyPermission(
                1001L,
                "阈值策略维护",
                GovernancePermissionCodes.RULE_DEFINITION_EDIT
        ));
    }

    @Test
    void requireAnyPermissionShouldPassWhenSuperAdmin() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);
        UserAuthContextVO context = new UserAuthContextVO();
        context.setSuperAdmin(true);
        when(permissionService.getUserAuthContext(1001L)).thenReturn(context);

        assertDoesNotThrow(() -> guard.requireAnyPermission(
                1001L,
                "阈值策略维护",
                GovernancePermissionCodes.RULE_DEFINITION_EDIT
        ));
    }

    @Test
    void requireAnyPermissionShouldThrowWhenPermissionMissing() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);
        UserAuthContextVO context = new UserAuthContextVO();
        context.setPermissions(List.of(GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE));
        when(permissionService.getUserAuthContext(1001L)).thenReturn(context);

        assertThrows(BizException.class, () -> guard.requireAnyPermission(
                1001L,
                "阈值策略维护",
                GovernancePermissionCodes.RULE_DEFINITION_EDIT
        ));
    }

    @Test
    void requireDualControlShouldPassWhenOperatorAndApproverAreSeparated() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);
        UserAuthContextVO operator = new UserAuthContextVO();
        operator.setPermissions(List.of(GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE));
        UserAuthContextVO approver = new UserAuthContextVO();
        approver.setPermissions(List.of(GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE));
        when(permissionService.getUserAuthContext(1001L)).thenReturn(operator);
        when(permissionService.getUserAuthContext(2001L)).thenReturn(approver);

        assertDoesNotThrow(() -> guard.requireDualControl(
                1001L,
                2001L,
                "产品契约发布",
                GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE,
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        ));
    }

    @Test
    void requireDualControlShouldRejectSameOperatorAndApprover() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);

        assertThrows(BizException.class, () -> guard.requireDualControl(
                1001L,
                1001L,
                "产品契约发布",
                GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE,
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        ));
    }

    @Test
    void requireDualControlShouldRejectApproverWithoutPermission() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);
        UserAuthContextVO operator = new UserAuthContextVO();
        operator.setPermissions(List.of(GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE));
        UserAuthContextVO approver = new UserAuthContextVO();
        approver.setPermissions(List.of(GovernancePermissionCodes.NORMATIVE_LIBRARY_WRITE));
        when(permissionService.getUserAuthContext(1001L)).thenReturn(operator);
        when(permissionService.getUserAuthContext(2001L)).thenReturn(approver);

        assertThrows(BizException.class, () -> guard.requireDualControl(
                1001L,
                2001L,
                "产品契约发布",
                GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE,
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        ));
    }
}
