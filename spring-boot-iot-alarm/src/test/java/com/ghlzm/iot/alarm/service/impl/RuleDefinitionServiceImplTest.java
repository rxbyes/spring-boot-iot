package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RuleDefinitionServiceImplTest {

    @Test
    void addRuleShouldRejectEnabledRuleWithoutExpression() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("空表达式策略");
        rule.setMetricIdentifier("dispsX");
        rule.setStatus(0);

        BizException error = assertThrows(BizException.class, () -> service.addRule(rule));

        assertEquals("启用中的阈值策略必须提供可执行表达式", error.getMessage());
        verify(service, never()).save(ArgumentMatchers.any(RuleDefinition.class));
    }

    @Test
    void updateRuleShouldRejectEnabledRuleWithInvalidExpression() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
        RuleDefinition rule = new RuleDefinition();
        rule.setId(9102L);
        rule.setRuleName("非法表达式策略");
        rule.setMetricIdentifier("dispsX");
        rule.setExpression("value >< 5");
        rule.setStatus(0);

        BizException error = assertThrows(BizException.class, () -> service.updateRule(rule));

        assertEquals("阈值策略表达式格式无效，仅支持 value >= 12 这类写法", error.getMessage());
        verify(service, never()).updateById(ArgumentMatchers.any(RuleDefinition.class));
    }

    @Test
    void addRuleShouldPersistWhenExpressionIsExecutable() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("红色策略");
        rule.setMetricIdentifier("dispsX");
        rule.setExpression("value >= 12");
        rule.setStatus(0);

        doReturn(true).when(service).save(ArgumentMatchers.any(RuleDefinition.class));

        service.addRule(rule);

        assertEquals(0, rule.getDeleted());
        verify(service).save(rule);
    }
}
