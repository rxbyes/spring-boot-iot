package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.framework.config.IotProperties;

import java.math.BigDecimal;

/**
 * 风险策略解析结果。
 */
public class RiskPolicyDecision {

    public static final String SOURCE_RULE_DEFINITION = "RULE_DEFINITION";
    public static final String SOURCE_AUTO_CLOSURE = "AUTO_CLOSURE";

    private final String source;
    private final Long ruleId;
    private final String ruleName;
    private final String thresholdText;
    private final Integer duration;
    private final AutoClosureSeverity severity;
    private final boolean createEvent;

    private RiskPolicyDecision(String source,
                               Long ruleId,
                               String ruleName,
                               String thresholdText,
                               Integer duration,
                               AutoClosureSeverity severity,
                               boolean createEvent) {
        this.source = source;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.thresholdText = thresholdText;
        this.duration = duration;
        this.severity = severity;
        this.createEvent = createEvent;
    }

    public static RiskPolicyDecision fromRule(RuleDefinition rule) {
        AutoClosureSeverity severity = mapRuleSeverity(rule == null ? null : rule.getAlarmLevel());
        boolean createEvent = rule != null && rule.getConvertToEvent() != null
                ? Integer.valueOf(1).equals(rule.getConvertToEvent())
                : severity.shouldCreateEvent();
        return new RiskPolicyDecision(
                SOURCE_RULE_DEFINITION,
                rule == null ? null : rule.getId(),
                rule == null ? null : rule.getRuleName(),
                rule == null ? null : rule.getExpression(),
                rule == null ? null : rule.getDuration(),
                severity,
                createEvent
        );
    }

    public static RiskPolicyDecision fromAutoClosure(BigDecimal absoluteValue, IotProperties.Alarm.AutoClosure config) {
        AutoClosureSeverity severity = AutoClosureSeverity.classify(absoluteValue, config);
        return new RiskPolicyDecision(
                SOURCE_AUTO_CLOSURE,
                null,
                null,
                buildAutoThresholdText(severity, config),
                null,
                severity,
                severity.shouldCreateEvent()
        );
    }

    private static AutoClosureSeverity mapRuleSeverity(String rawLevel) {
        if (rawLevel == null || rawLevel.isBlank()) {
            return AutoClosureSeverity.YELLOW;
        }
        String normalized = rawLevel.trim().toLowerCase();
        return switch (normalized) {
            case "critical", "red" -> AutoClosureSeverity.RED;
            case "high", "warning", "warn", "orange" -> AutoClosureSeverity.ORANGE;
            case "medium", "yellow" -> AutoClosureSeverity.YELLOW;
            case "low", "info", "blue" -> AutoClosureSeverity.BLUE;
            default -> AutoClosureSeverity.YELLOW;
        };
    }

    private static String buildAutoThresholdText(AutoClosureSeverity severity, IotProperties.Alarm.AutoClosure config) {
        BigDecimal yellow = config == null || config.getYellow() == null ? BigDecimal.valueOf(5) : config.getYellow();
        BigDecimal orange = config == null || config.getOrange() == null ? BigDecimal.valueOf(10) : config.getOrange();
        BigDecimal red = config == null || config.getRed() == null ? BigDecimal.valueOf(20) : config.getRed();
        return switch (severity) {
            case YELLOW -> "[" + toPlainString(yellow) + ", " + toPlainString(orange) + ") mm";
            case ORANGE -> "[" + toPlainString(orange) + ", " + toPlainString(red) + ") mm";
            case RED -> ">= " + toPlainString(red) + " mm";
            case BLUE -> "< " + toPlainString(yellow) + " mm";
        };
    }

    private static String toPlainString(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    public String getSource() {
        return source;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getThresholdText() {
        return thresholdText;
    }

    public Integer getDuration() {
        return duration;
    }

    public AutoClosureSeverity getSeverity() {
        return severity;
    }

    public String getAlarmLevel() {
        return severity.getAlarmLevel();
    }

    public String getRiskPointLevel() {
        return severity.getRiskPointLevel();
    }

    public String getColorCode() {
        return severity.getColorCode();
    }

    public String getColorLabel() {
        return severity.getColorLabel();
    }

    public int getPriority() {
        return severity.getPriority();
    }

    public boolean shouldCreateAlarm() {
        return severity.shouldCreateAlarm();
    }

    public boolean shouldCreateEvent() {
        return createEvent;
    }
}
