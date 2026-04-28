package com.ghlzm.iot.alarm.auto;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class RiskObjectSituationResolver {

    private final RiskObjectSituationPolicy policy;

    public RiskObjectSituationResolver() {
        this(RiskObjectSituationPolicy.defaultPolicy());
    }

    RiskObjectSituationResolver(RiskObjectSituationPolicy policy) {
        this.policy = policy == null ? RiskObjectSituationPolicy.defaultPolicy() : policy;
    }

    public RiskObjectSituationDecision resolve(int totalBindingCount, List<RiskObjectSignal> signals) {
        List<RiskObjectSignal> safeSignals = signals == null ? List.of() : signals;
        int effectiveTotal = Math.max(totalBindingCount, safeSignals.size());
        List<RiskObjectSignal> activeSignals = safeSignals.stream()
                .filter(RiskObjectSignal::isActive)
                .toList();
        RiskObjectSignal responseSignal = activeSignals.stream()
                .filter(signal -> signal.getDecision() != null && signal.getDecision().shouldCreateAlarm())
                .max(Comparator.comparingInt(signal -> signal.getDecision().getPriority()))
                .orElse(null);
        RiskPolicyDecision responseDecision = responseSignal == null ? null : responseSignal.getDecision();
        boolean createSignalAlarm = responseDecision != null && responseDecision.shouldCreateAlarm();
        boolean triggerResponse = shouldTriggerResponse(effectiveTotal, activeSignals.size(), responseDecision);
        return new RiskObjectSituationDecision(
                effectiveTotal,
                activeSignals.size(),
                reasonCode(effectiveTotal, activeSignals.size(), triggerResponse),
                responseDecision,
                responseSignal,
                activeSignals,
                createSignalAlarm,
                triggerResponse
        );
    }

    private boolean shouldTriggerResponse(int totalBindingCount, int activeSignalCount, RiskPolicyDecision responseDecision) {
        if (responseDecision == null || !responseDecision.shouldCreateEvent()) {
            return false;
        }
        return activeSignalCount >= policy.responseThresholdFor(totalBindingCount);
    }

    private String reasonCode(int totalBindingCount, int activeSignalCount, boolean triggerResponse) {
        if (activeSignalCount == 0) {
            return "NO_ACTIVE_SIGNAL";
        }
        if (totalBindingCount <= 1) {
            return triggerResponse ? "SINGLE_BINDING_COMPAT" : "SINGLE_BINDING_SIGNAL";
        }
        if (triggerResponse) {
            return "CONFIRMED_MULTI_SIGNAL";
        }
        return "SINGLE_SIGNAL_ONLY";
    }
}
