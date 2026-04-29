package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.alarm.entity.RiskPointDevice;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class RiskObjectSignal {

    private final Long bindingId;
    private final Long deviceId;
    private final String deviceCode;
    private final String deviceName;
    private final String metricIdentifier;
    private final String metricName;
    private final BigDecimal value;
    private final RiskPolicyDecision decision;
    private final boolean active;

    private RiskObjectSignal(Long bindingId,
                             Long deviceId,
                             String deviceCode,
                             String deviceName,
                             String metricIdentifier,
                             String metricName,
                             BigDecimal value,
                             RiskPolicyDecision decision,
                             boolean active) {
        this.bindingId = bindingId;
        this.deviceId = deviceId;
        this.deviceCode = deviceCode;
        this.deviceName = deviceName;
        this.metricIdentifier = metricIdentifier;
        this.metricName = metricName;
        this.value = value;
        this.decision = decision;
        this.active = active;
    }

    public static RiskObjectSignal active(Long bindingId,
                                          Long deviceId,
                                          String metricIdentifier,
                                          BigDecimal value,
                                          RiskPolicyDecision decision) {
        return new RiskObjectSignal(bindingId, deviceId, null, null, metricIdentifier, null, value, decision,
                decision != null && decision.shouldCreateAlarm());
    }

    public static RiskObjectSignal active(RiskPointDevice binding,
                                          BigDecimal value,
                                          RiskPolicyDecision decision) {
        return new RiskObjectSignal(
                binding == null ? null : binding.getId(),
                binding == null ? null : binding.getDeviceId(),
                binding == null ? null : binding.getDeviceCode(),
                binding == null ? null : binding.getDeviceName(),
                binding == null ? null : binding.getMetricIdentifier(),
                binding == null ? null : binding.getMetricName(),
                value,
                decision,
                decision != null && decision.shouldCreateAlarm()
        );
    }

    public Long getBindingId() {
        return bindingId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getMetricIdentifier() {
        return metricIdentifier;
    }

    public String getMetricName() {
        return metricName;
    }

    public BigDecimal getValue() {
        return value;
    }

    public RiskPolicyDecision getDecision() {
        return decision;
    }

    public boolean isActive() {
        return active;
    }

    public Map<String, Object> toSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("bindingId", bindingId);
        summary.put("deviceId", deviceId);
        summary.put("deviceCode", deviceCode);
        summary.put("deviceName", deviceName);
        summary.put("metricIdentifier", metricIdentifier);
        summary.put("metricName", metricName);
        summary.put("value", value == null ? null : value.stripTrailingZeros().toPlainString());
        summary.put("active", active);
        summary.put("level", decision == null ? null : decision.getAlarmLevel());
        summary.put("policySource", decision == null ? null : decision.getSource());
        summary.put("ruleId", decision == null ? null : decision.getRuleId());
        return summary;
    }
}
