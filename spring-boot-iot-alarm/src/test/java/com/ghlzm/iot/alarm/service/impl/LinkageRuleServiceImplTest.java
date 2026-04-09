package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LinkageRuleServiceImplTest {

    @Mock
    private RiskMetricActionBindingSyncService bindingSyncService;

    @Test
    void addRuleShouldPersistRuleAndTriggerBindingSyncWithCurrentOperator() {
        LinkageRuleServiceImpl service = spy(new LinkageRuleServiceImpl(bindingSyncService));
        LinkageRule rule = new LinkageRule();
        rule.setRuleName("裂缝联动");
        doAnswer(invocation -> {
            LinkageRule saved = invocation.getArgument(0);
            saved.setId(7001L);
            return true;
        }).when(service).save(any(LinkageRule.class));

        service.addRule(rule, 1001L);

        verify(bindingSyncService).rebuildLinkageBindingsForRule(rule, 1001L, "AUTO_INFERRED");
        assertEquals(1001L, rule.getCreateBy());
        assertEquals(1001L, rule.getUpdateBy());
    }
}
