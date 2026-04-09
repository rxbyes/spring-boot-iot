package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 风险运行态等级解析器。
 */
@Component
public class RiskRuntimeLevelResolver {

    public String resolve(RiskPoint riskPoint, AlarmRecord activeAlarm, EventRecord recentEvent) {
        if (activeAlarm != null) {
            return mapAlarmLevelToRiskLevel(activeAlarm.getAlarmLevel());
        }
        if (recentEvent != null && StringUtils.hasText(recentEvent.getRiskLevel())) {
            return normalizeRiskLevel(recentEvent.getRiskLevel());
        }
        if (riskPoint == null) {
            return null;
        }
        if (StringUtils.hasText(riskPoint.getCurrentRiskLevel())) {
            return normalizeRiskLevel(riskPoint.getCurrentRiskLevel());
        }
        return normalizeRiskLevel(riskPoint.getRiskLevel());
    }

    public int priorityForAlarmLevel(String alarmLevel) {
        return priorityForRiskLevel(mapAlarmLevelToRiskLevel(alarmLevel));
    }

    public int priorityForRiskLevel(String riskLevel) {
        String normalized = normalizeRiskLevel(riskLevel);
        return switch (normalized) {
            case "red" -> 3;
            case "orange" -> 2;
            case "yellow" -> 1;
            default -> 0;
        };
    }

    private String mapAlarmLevelToRiskLevel(String alarmLevel) {
        if (!StringUtils.hasText(alarmLevel)) {
            return "blue";
        }
        String normalized = alarmLevel.trim().toLowerCase();
        return switch (normalized) {
            case "critical", "red" -> "red";
            case "high", "warning", "warn", "orange" -> "orange";
            case "medium", "yellow" -> "yellow";
            default -> "blue";
        };
    }

    private String normalizeRiskLevel(String riskLevel) {
        if (!StringUtils.hasText(riskLevel)) {
            return "blue";
        }
        String normalized = riskLevel.trim().toLowerCase();
        return switch (normalized) {
            case "critical" -> "red";
            case "warning", "warn", "high" -> "orange";
            case "medium" -> "yellow";
            case "low", "info" -> "blue";
            default -> normalized;
        };
    }
}
