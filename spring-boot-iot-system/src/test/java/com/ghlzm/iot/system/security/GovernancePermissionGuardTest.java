package com.ghlzm.iot.system.security;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
        context.setPermissions(List.of("risk:rule-definition:write"));
        when(permissionService.getUserAuthContext(1001L)).thenReturn(context);

        assertDoesNotThrow(() -> guard.requireAnyPermission(
                1001L,
                "阈值策略维护",
                "risk:rule-definition:write"
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
                "risk:rule-definition:write"
        ));
    }

    @Test
    void requireAnyPermissionShouldThrowWhenPermissionMissing() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);
        UserAuthContextVO context = new UserAuthContextVO();
        context.setPermissions(List.of("iot:products:update"));
        when(permissionService.getUserAuthContext(1001L)).thenReturn(context);

        assertThrows(BizException.class, () -> guard.requireAnyPermission(
                1001L,
                "阈值策略维护",
                "risk:rule-definition:write"
        ));
    }

    @Test
    void requireDualControlShouldPassWhenOperatorAndApproverAreSeparated() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);
        UserAuthContextVO operator = new UserAuthContextVO();
        operator.setPermissions(List.of("iot:product-contract:release"));
        UserAuthContextVO approver = new UserAuthContextVO();
        approver.setPermissions(List.of("iot:product-contract:approve"));
        when(permissionService.getUserAuthContext(1001L)).thenReturn(operator);
        when(permissionService.getUserAuthContext(2001L)).thenReturn(approver);

        assertDoesNotThrow(() -> guard.requireDualControl(
                1001L,
                2001L,
                "产品契约发布",
                "iot:product-contract:release",
                "iot:product-contract:approve",
                "iot:products:update"
        ));
    }

    @Test
    void requireDualControlShouldRejectSameOperatorAndApprover() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);

        assertThrows(BizException.class, () -> guard.requireDualControl(
                1001L,
                1001L,
                "产品契约发布",
                "iot:product-contract:release",
                "iot:product-contract:approve",
                "iot:products:update"
        ));
    }

    @Test
    void requireDualControlShouldRejectApproverWithoutPermission() {
        GovernancePermissionGuard guard = new GovernancePermissionGuard(permissionService);
        UserAuthContextVO operator = new UserAuthContextVO();
        operator.setPermissions(List.of("iot:product-contract:release"));
        UserAuthContextVO approver = new UserAuthContextVO();
        approver.setPermissions(List.of("iot:normative-library:write"));
        when(permissionService.getUserAuthContext(1001L)).thenReturn(operator);
        when(permissionService.getUserAuthContext(2001L)).thenReturn(approver);

        assertThrows(BizException.class, () -> guard.requireDualControl(
                1001L,
                2001L,
                "产品契约发布",
                "iot:product-contract:release",
                "iot:product-contract:approve",
                "iot:products:update"
        ));
    }
}
