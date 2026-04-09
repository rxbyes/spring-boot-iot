package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.service.EmergencyPlanService;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmergencyPlanControllerTest {

    @Mock
    private EmergencyPlanService emergencyPlanService;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    private EmergencyPlanController controller;

    @BeforeEach
    void setUp() {
        controller = new EmergencyPlanController(emergencyPlanService, permissionGuard);
    }

    @Test
    void addPlanShouldRequireDualControl() {
        EmergencyPlan plan = new EmergencyPlan();

        controller.addPlan(plan, 2002L, authentication(1001L));

        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "emergency-plan-create",
                "risk:emergency-plan:edit",
                "risk:emergency-plan:approve"
        );
        verify(emergencyPlanService).addPlan(plan, 1001L);
    }

    @Test
    void updatePlanShouldRequireDualControl() {
        EmergencyPlan plan = new EmergencyPlan();

        controller.updatePlan(plan, 2002L, authentication(1001L));

        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "emergency-plan-update",
                "risk:emergency-plan:edit",
                "risk:emergency-plan:approve"
        );
        verify(emergencyPlanService).updatePlan(plan, 1001L);
    }

    @Test
    void deletePlanShouldRequireDualControl() {
        R<Void> response = controller.deletePlan(3001L, 2002L, authentication(1001L));

        assertNull(response.getData());
        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "emergency-plan-delete",
                "risk:emergency-plan:edit",
                "risk:emergency-plan:approve"
        );
        verify(emergencyPlanService).deletePlan(3001L, 1001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
