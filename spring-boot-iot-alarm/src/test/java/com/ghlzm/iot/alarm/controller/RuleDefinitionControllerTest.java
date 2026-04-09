package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
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
class RuleDefinitionControllerTest {

    @Mock
    private RuleDefinitionService ruleDefinitionService;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    private RuleDefinitionController controller;

    @BeforeEach
    void setUp() {
        controller = new RuleDefinitionController(ruleDefinitionService, permissionGuard);
    }

    @Test
    void addRuleShouldRequireDualControl() {
        RuleDefinition rule = new RuleDefinition();

        controller.addRule(rule, 2002L, authentication(1001L));

        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "rule-definition-create",
                "risk:rule-definition:edit",
                "risk:rule-definition:approve"
        );
        verify(ruleDefinitionService).addRule(rule);
    }

    @Test
    void updateRuleShouldRequireDualControl() {
        RuleDefinition rule = new RuleDefinition();

        controller.updateRule(rule, 2002L, authentication(1001L));

        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "rule-definition-update",
                "risk:rule-definition:edit",
                "risk:rule-definition:approve"
        );
        verify(ruleDefinitionService).updateRule(rule);
    }

    @Test
    void deleteRuleShouldRequireDualControl() {
        R<Void> response = controller.deleteRule(3001L, 2002L, authentication(1001L));

        assertNull(response.getData());
        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "rule-definition-delete",
                "risk:rule-definition:edit",
                "risk:rule-definition:approve"
        );
        verify(ruleDefinitionService).deleteRule(3001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
