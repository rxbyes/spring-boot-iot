package com.ghlzm.iot.alarm.auto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RiskObjectSituationDecision {

    private final int totalBindingCount;
    private final int activeSignalCount;
    private final String reasonCode;
    private final RiskPolicyDecision responseDecision;
    private final RiskObjectSignal responseSignal;
    private final List<RiskObjectSignal> activeSignals;
    private final boolean createSignalAlarm;
    private final boolean triggerResponse;

    RiskObjectSituationDecision(int totalBindingCount,
                                int activeSignalCount,
                                String reasonCode,
                                RiskPolicyDecision responseDecision,
                                RiskObjectSignal responseSignal,
                                List<RiskObjectSignal> activeSignals,
                                boolean createSignalAlarm,
                                boolean triggerResponse) {
        this.totalBindingCount = totalBindingCount;
        this.activeSignalCount = activeSignalCount;
        this.reasonCode = reasonCode;
        this.responseDecision = responseDecision;
        this.responseSignal = responseSignal;
        this.activeSignals = activeSignals == null ? List.of() : List.copyOf(activeSignals);
        this.createSignalAlarm = createSignalAlarm;
        this.triggerResponse = triggerResponse;
    }

    public int getTotalBindingCount() {
        return totalBindingCount;
    }

    public int getActiveSignalCount() {
        return activeSignalCount;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public RiskPolicyDecision getResponseDecision() {
        return responseDecision;
    }

    public RiskObjectSignal getResponseSignal() {
        return responseSignal;
    }

    public List<RiskObjectSignal> getActiveSignals() {
        return activeSignals;
    }

    public boolean shouldCreateSignalAlarm() {
        return createSignalAlarm;
    }

    public boolean shouldTriggerResponse() {
        return triggerResponse;
    }

    public Map<String, Object> toSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBindingCount", totalBindingCount);
        summary.put("activeSignalCount", activeSignalCount);
        summary.put("reasonCode", reasonCode);
        summary.put("triggerResponse", triggerResponse);
        summary.put("responseLevel", responseDecision == null ? null : responseDecision.getAlarmLevel());
        summary.put("responseRuleId", responseDecision == null ? null : responseDecision.getRuleId());
        summary.put("responseSignal", responseSignal == null ? null : responseSignal.toSummary());
        summary.put("activeSignals", activeSignals.stream().map(RiskObjectSignal::toSummary).toList());
        return summary;
    }
}
