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
}
