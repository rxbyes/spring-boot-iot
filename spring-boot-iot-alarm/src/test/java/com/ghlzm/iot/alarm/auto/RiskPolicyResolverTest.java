package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskPolicyResolverTest {

    @Mock
    private RuleDefinitionMapper ruleDefinitionMapper;

    private RiskPolicyResolver resolver;

    @BeforeEach
    void setUp() {
        IotProperties properties = new IotProperties();
        IotProperties.Alarm alarm = new IotProperties.Alarm();
        IotProperties.Alarm.AutoClosure autoClosure = new IotProperties.Alarm.AutoClosure();
        autoClosure.setEnabled(true);
        autoClosure.setYellow(BigDecimal.valueOf(5));
        autoClosure.setOrange(BigDecimal.valueOf(10));
        autoClosure.setRed(BigDecimal.valueOf(20));
        alarm.setAutoClosure(autoClosure);
        properties.setAlarm(alarm);
        resolver = new RiskPolicyResolver(ruleDefinitionMapper, properties);
    }

    @Test
    void resolveShouldPreferEnabledRuleDefinitionOverYamlFallback() {
        RuleDefinition rule = new RuleDefinition();
        rule.setId(9101L);
        rule.setRuleName("深部位移红色策略");
        rule.setMetricIdentifier("dispsX");
        rule.setExpression("value >= 12");
        rule.setAlarmLevel("critical");
        rule.setConvertToEvent(1);
        rule.setStatus(0);
        rule.setDeleted(0);

        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule));

        RiskPolicyDecision decision = resolver.resolve(1L, binding("dispsX"), new BigDecimal("12.8"));

        assertEquals("RULE_DEFINITION", decision.getSource());
        assertEquals("critical", decision.getAlarmLevel());
        assertEquals("red", decision.getRiskPointLevel());
        assertEquals(9101L, decision.getRuleId());
        assertTrue(decision.shouldCreateAlarm());
        assertTrue(decision.shouldCreateEvent());
    }

    @Test
    void resolveShouldFallbackToAutoClosureThresholdWhenNoEnabledRuleMatches() {
        RuleDefinition disabledRule = new RuleDefinition();
        disabledRule.setMetricIdentifier("dispsX");
        disabledRule.setExpression("value >= 12");
        disabledRule.setAlarmLevel("critical");
        disabledRule.setStatus(1);
        disabledRule.setDeleted(0);

        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(disabledRule));

        RiskPolicyDecision decision = resolver.resolve(1L, binding("dispsX"), new BigDecimal("7.5"));

        assertEquals("AUTO_CLOSURE", decision.getSource());
        assertEquals("medium", decision.getAlarmLevel());
        assertEquals("yellow", decision.getRiskPointLevel());
        assertTrue(decision.shouldCreateAlarm());
        assertFalse(decision.shouldCreateEvent());
    }

    private RiskPointDevice binding(String metricIdentifier) {
        RiskPointDevice binding = new RiskPointDevice();
        binding.setMetricIdentifier(metricIdentifier);
        binding.setMetricName("顺滑动方向累计变形量");
        binding.setThresholdUnit("mm");
        return binding;
    }
}
