package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import com.ghlzm.iot.alarm.vo.RuleDefinitionEffectivePreviewVO;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleDefinitionControllerTest {

    @Mock
    private RuleDefinitionService ruleDefinitionService;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    @Mock
    private GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver;

    private RuleDefinitionController controller;

    @BeforeEach
    void setUp() {
        controller = new RuleDefinitionController(ruleDefinitionService, permissionGuard, governanceApprovalPolicyResolver);
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

    @Test
    void deleteRuleShouldResolveFixedApproverWhenHeaderIsMissing() {
        when(governanceApprovalPolicyResolver.resolveApproverUserId("RULE_DEFINITION_DELETE", 1001L))
                .thenReturn(99000001L);

        R<Void> response = controller.deleteRule(3001L, null, authentication(1001L));

        assertNull(response.getData());
        verify(governanceApprovalPolicyResolver).resolveApproverUserId("RULE_DEFINITION_DELETE", 1001L);
        verify(permissionGuard).requireDualControl(
                1001L,
                99000001L,
                "rule-definition-delete",
                "risk:rule-definition:edit",
                "risk:rule-definition:approve"
        );
        verify(ruleDefinitionService).deleteRule(3001L);
    }

    @Test
    void previewEffectiveRuleShouldDelegateToServiceWithoutDualControl() {
        RuleDefinitionEffectivePreviewVO preview = new RuleDefinitionEffectivePreviewVO();
        when(ruleDefinitionService.previewEffectiveRule(1L, 6102L, "value", 1001L, "MONITORING", 8001L, 9001L))
                .thenReturn(preview);

        R<RuleDefinitionEffectivePreviewVO> response = controller.previewEffectiveRule(
                1L,
                6102L,
                "value",
                1001L,
                "MONITORING",
                8001L,
                9001L
        );

        assertSame(preview, response.getData());
        verify(ruleDefinitionService).previewEffectiveRule(1L, 6102L, "value", 1001L, "MONITORING", 8001L, 9001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
