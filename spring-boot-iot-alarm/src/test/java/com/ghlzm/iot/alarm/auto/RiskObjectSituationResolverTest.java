package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskObjectSituationResolverTest {

    private final RiskObjectSituationResolver resolver = new RiskObjectSituationResolver();

    @Test
    void defaultPolicyShouldRequireOneSignalForSingleBindingAndTwoForMultiBinding() {
        RiskObjectSituationPolicy policy = RiskObjectSituationPolicy.defaultPolicy();

        assertEquals(1, policy.responseThresholdFor(1));
        assertEquals(2, policy.responseThresholdFor(2));
        assertEquals(2, policy.responseThresholdFor(5));
    }

    @Test
    void resolveShouldKeepSingleSignalAsEvidenceOnlyForMultiMetricRiskObject() {
        RiskObjectSituationDecision decision = resolver.resolve(2, List.of(
                RiskObjectSignal.active(9001L, 3001L, "dispsX", new BigDecimal("12.4"), ruleDecision(11L, "orange"))
        ));

        assertTrue(decision.shouldCreateSignalAlarm());
        assertFalse(decision.shouldTriggerResponse());
        assertEquals("SINGLE_SIGNAL_ONLY", decision.getReasonCode());
        assertEquals(1, decision.getActiveSignalCount());
        assertEquals(2, decision.getTotalBindingCount());
    }

    @Test
    void resolveShouldUseCustomMultiBindingResponseThreshold() {
        RiskObjectSituationResolver customResolver = new RiskObjectSituationResolver(
                RiskObjectSituationPolicy.withMultiBindingResponseThreshold(3)
        );

        RiskObjectSituationDecision twoSignals = customResolver.resolve(4, List.of(
                RiskObjectSignal.active(9001L, 3001L, "dispsX", new BigDecimal("12.4"), ruleDecision(11L, "orange")),
                RiskObjectSignal.active(9002L, 3002L, "dispsY", new BigDecimal("22.1"), ruleDecision(12L, "red"))
        ));
        RiskObjectSituationDecision threeSignals = customResolver.resolve(4, List.of(
                RiskObjectSignal.active(9001L, 3001L, "dispsX", new BigDecimal("12.4"), ruleDecision(11L, "orange")),
                RiskObjectSignal.active(9002L, 3002L, "dispsY", new BigDecimal("22.1"), ruleDecision(12L, "red")),
                RiskObjectSignal.active(9003L, 3003L, "dispsZ", new BigDecimal("32.1"), ruleDecision(13L, "red"))
        ));

        assertTrue(twoSignals.shouldCreateSignalAlarm());
        assertFalse(twoSignals.shouldTriggerResponse());
        assertEquals("SINGLE_SIGNAL_ONLY", twoSignals.getReasonCode());
        assertTrue(threeSignals.shouldTriggerResponse());
        assertEquals("CONFIRMED_MULTI_SIGNAL", threeSignals.getReasonCode());
    }

    @Test
    void resolveShouldTriggerResponseWhenTwoSignalsAreActiveForSameRiskObject() {
        RiskObjectSituationDecision decision = resolver.resolve(3, List.of(
                RiskObjectSignal.active(9001L, 3001L, "dispsX", new BigDecimal("12.4"), ruleDecision(11L, "orange")),
                RiskObjectSignal.active(9002L, 3002L, "dispsY", new BigDecimal("22.1"), ruleDecision(12L, "red"))
        ));

        assertTrue(decision.shouldCreateSignalAlarm());
        assertTrue(decision.shouldTriggerResponse());
        assertEquals("CONFIRMED_MULTI_SIGNAL", decision.getReasonCode());
        assertEquals("red", decision.getResponseDecision().getAlarmLevel());
        assertEquals(2, decision.getActiveSignalCount());
        assertEquals(3, decision.getTotalBindingCount());
    }

    @Test
    void resolveShouldKeepSingleBindingRiskObjectCompatibleWithExistingClosure() {
        RiskObjectSituationDecision decision = resolver.resolve(1, List.of(
                RiskObjectSignal.active(9001L, 3001L, "dispsX", new BigDecimal("22.4"), ruleDecision(11L, "red"))
        ));

        assertTrue(decision.shouldCreateSignalAlarm());
        assertTrue(decision.shouldTriggerResponse());
        assertEquals("SINGLE_BINDING_COMPAT", decision.getReasonCode());
    }

    private RiskPolicyDecision ruleDecision(Long ruleId, String alarmLevel) {
        RuleDefinition rule = new RuleDefinition();
        rule.setId(ruleId);
        rule.setRuleName("test rule " + ruleId);
        rule.setExpression("value >= 1");
        rule.setAlarmLevel(alarmLevel);
        rule.setConvertToEvent(1);
        return RiskPolicyDecision.fromRule(rule);
    }
}
