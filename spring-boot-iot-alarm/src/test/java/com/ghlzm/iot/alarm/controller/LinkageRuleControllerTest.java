package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.service.LinkageRuleService;
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
class LinkageRuleControllerTest {

    @Mock
    private LinkageRuleService linkageRuleService;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    private LinkageRuleController controller;

    @BeforeEach
    void setUp() {
        controller = new LinkageRuleController(linkageRuleService, permissionGuard);
    }

    @Test
    void addRuleShouldRequireDualControl() {
        LinkageRule rule = new LinkageRule();

        controller.addRule(rule, 2002L, authentication(1001L));

        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "linkage-rule-create",
                "risk:linkage-rule:edit",
                "risk:linkage-rule:approve"
        );
        verify(linkageRuleService).addRule(rule);
    }

    @Test
    void updateRuleShouldRequireDualControl() {
        LinkageRule rule = new LinkageRule();

        controller.updateRule(rule, 2002L, authentication(1001L));

        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "linkage-rule-update",
                "risk:linkage-rule:edit",
                "risk:linkage-rule:approve"
        );
        verify(linkageRuleService).updateRule(rule);
    }

    @Test
    void deleteRuleShouldRequireDualControl() {
        R<Void> response = controller.deleteRule(3001L, 2002L, authentication(1001L));

        assertNull(response.getData());
        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "linkage-rule-delete",
                "risk:linkage-rule:edit",
                "risk:linkage-rule:approve"
        );
        verify(linkageRuleService).deleteRule(3001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
