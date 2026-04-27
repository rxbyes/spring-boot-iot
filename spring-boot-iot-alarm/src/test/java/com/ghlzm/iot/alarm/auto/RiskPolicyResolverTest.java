package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
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

    @Mock
    private DeviceMapper deviceMapper;

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
        resolver = new RiskPolicyResolver(ruleDefinitionMapper, properties, deviceMapper);
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
        assertEquals("red", decision.getAlarmLevel());
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
        assertEquals("yellow", decision.getAlarmLevel());
        assertEquals("yellow", decision.getRiskPointLevel());
        assertTrue(decision.shouldCreateAlarm());
        assertFalse(decision.shouldCreateEvent());
    }

    @Test
    void resolveShouldPreferHighestSeverityWhenMultipleRulesMatchSameMetric() {
        RuleDefinition orangeRule = new RuleDefinition();
        orangeRule.setId(8217L);
        orangeRule.setRuleName("深部位移橙色策略");
        orangeRule.setMetricIdentifier("dispsY");
        orangeRule.setExpression(">= 10");
        orangeRule.setAlarmLevel("orange");
        orangeRule.setConvertToEvent(1);
        orangeRule.setStatus(0);
        orangeRule.setDeleted(0);

        RuleDefinition redRule = new RuleDefinition();
        redRule.setId(8218L);
        redRule.setRuleName("深部位移红色策略");
        redRule.setMetricIdentifier("dispsY");
        redRule.setExpression(">= 20");
        redRule.setAlarmLevel("critical");
        redRule.setConvertToEvent(1);
        redRule.setStatus(0);
        redRule.setDeleted(0);

        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(orangeRule, redRule));

        RiskPolicyDecision decision = resolver.resolve(1L, binding("dispsY"), new BigDecimal("21.6"));

        assertEquals("RULE_DEFINITION", decision.getSource());
        assertEquals("red", decision.getAlarmLevel());
        assertEquals("red", decision.getRiskPointLevel());
        assertEquals(8218L, decision.getRuleId());
        assertTrue(decision.shouldCreateEvent());
    }

    @Test
    void resolveShouldMatchEnabledRuleByRiskMetricId() {
        RuleDefinition rule = new RuleDefinition();
        rule.setId(9301L);
        rule.setRuleName("裂缝监测值红色策略");
        rule.setRiskMetricId(7001L);
        rule.setExpression("value >= 10");
        rule.setAlarmLevel("critical");
        rule.setConvertToEvent(1);
        rule.setStatus(0);
        rule.setDeleted(0);

        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule));

        RiskPointDevice binding = binding("value");
        binding.setRiskMetricId(7001L);
        RiskPolicyDecision decision = resolver.resolve(1L, binding, new BigDecimal("10.8"));

        assertEquals("RULE_DEFINITION", decision.getSource());
        assertEquals("red", decision.getAlarmLevel());
        assertEquals(9301L, decision.getRuleId());
    }

    @Test
    void resolveShouldUseProductDefaultRuleBeforeYamlFallback() {
        RuleDefinition productDefault = new RuleDefinition();
        productDefault.setId(9401L);
        productDefault.setRuleName("裂缝产品默认橙色策略");
        productDefault.setRuleScope("PRODUCT");
        productDefault.setProductId(1001L);
        productDefault.setRiskMetricId(7001L);
        productDefault.setMetricIdentifier("value");
        productDefault.setExpression("value >= 8");
        productDefault.setAlarmLevel("orange");
        productDefault.setConvertToEvent(1);
        productDefault.setStatus(0);
        productDefault.setDeleted(0);

        when(deviceMapper.selectById(8001L)).thenReturn(device(8001L, 1001L));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(productDefault));

        RiskPointDevice binding = binding("value");
        binding.setDeviceId(8001L);
        binding.setRiskMetricId(7001L);
        RiskPolicyDecision decision = resolver.resolve(1L, binding, new BigDecimal("8.2"));

        assertEquals("RULE_DEFINITION", decision.getSource());
        assertEquals("orange", decision.getAlarmLevel());
        assertEquals(9401L, decision.getRuleId());
    }

    @Test
    void resolveShouldPreferDevicePersonalizedRuleOverProductDefaultRule() {
        RuleDefinition productDefault = new RuleDefinition();
        productDefault.setId(9401L);
        productDefault.setRuleName("裂缝产品默认橙色策略");
        productDefault.setRuleScope("PRODUCT");
        productDefault.setProductId(1001L);
        productDefault.setRiskMetricId(7001L);
        productDefault.setMetricIdentifier("value");
        productDefault.setExpression("value >= 8");
        productDefault.setAlarmLevel("orange");
        productDefault.setConvertToEvent(1);
        productDefault.setStatus(0);
        productDefault.setDeleted(0);

        RuleDefinition deviceOverride = new RuleDefinition();
        deviceOverride.setId(9402L);
        deviceOverride.setRuleName("重点设备个性红色策略");
        deviceOverride.setRuleScope("DEVICE");
        deviceOverride.setDeviceId(8001L);
        deviceOverride.setRiskMetricId(7001L);
        deviceOverride.setMetricIdentifier("value");
        deviceOverride.setExpression("value >= 8");
        deviceOverride.setAlarmLevel("red");
        deviceOverride.setConvertToEvent(1);
        deviceOverride.setStatus(0);
        deviceOverride.setDeleted(0);

        when(deviceMapper.selectById(8001L)).thenReturn(device(8001L, 1001L));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(productDefault, deviceOverride));

        RiskPointDevice binding = binding("value");
        binding.setId(9001L);
        binding.setDeviceId(8001L);
        binding.setRiskMetricId(7001L);
        RiskPolicyDecision decision = resolver.resolve(1L, binding, new BigDecimal("8.2"));

        assertEquals("RULE_DEFINITION", decision.getSource());
        assertEquals("red", decision.getAlarmLevel());
        assertEquals(9402L, decision.getRuleId());
    }

    private RiskPointDevice binding(String metricIdentifier) {
        RiskPointDevice binding = new RiskPointDevice();
        binding.setMetricIdentifier(metricIdentifier);
        binding.setMetricName("顺滑动方向累计变形量");
        binding.setThresholdUnit("mm");
        return binding;
    }

    private Device device(Long deviceId, Long productId) {
        Device device = new Device();
        device.setId(deviceId);
        device.setProductId(productId);
        return device;
    }
}
