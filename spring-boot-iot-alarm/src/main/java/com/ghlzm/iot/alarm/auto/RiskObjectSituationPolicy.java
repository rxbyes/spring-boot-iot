package com.ghlzm.iot.alarm.auto;

public class RiskObjectSituationPolicy {

    private static final int SINGLE_BINDING_RESPONSE_THRESHOLD = 1;
    private static final int DEFAULT_MULTI_BINDING_RESPONSE_THRESHOLD = 2;

    private final int multiBindingResponseThreshold;

    private RiskObjectSituationPolicy(int multiBindingResponseThreshold) {
        if (multiBindingResponseThreshold < 2) {
            throw new IllegalArgumentException("multiBindingResponseThreshold must be at least 2");
        }
        this.multiBindingResponseThreshold = multiBindingResponseThreshold;
    }

    public static RiskObjectSituationPolicy defaultPolicy() {
        return new RiskObjectSituationPolicy(DEFAULT_MULTI_BINDING_RESPONSE_THRESHOLD);
    }

    public static RiskObjectSituationPolicy withMultiBindingResponseThreshold(int threshold) {
        return new RiskObjectSituationPolicy(threshold);
    }

    public int responseThresholdFor(int totalBindingCount) {
        if (totalBindingCount <= 1) {
            return SINGLE_BINDING_RESPONSE_THRESHOLD;
        }
        return multiBindingResponseThreshold;
    }
}
