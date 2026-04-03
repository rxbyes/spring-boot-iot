package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.framework.config.IotProperties;

import java.math.BigDecimal;

/**
 * 深部位移自动闭环等级。
 */
public enum AutoClosureSeverity {
    BLUE("blue", "蓝", "blue", "blue", false, false, 0),
    YELLOW("yellow", "黄", "yellow", "yellow", true, false, 1),
    ORANGE("orange", "橙", "orange", "orange", true, true, 2),
    RED("red", "红", "red", "red", true, true, 3);

    private final String colorCode;
    private final String colorLabel;
    private final String alarmLevel;
    private final String riskPointLevel;
    private final boolean createAlarm;
    private final boolean createEvent;
    private final int priority;

    AutoClosureSeverity(String colorCode,
                        String colorLabel,
                        String alarmLevel,
                        String riskPointLevel,
                        boolean createAlarm,
                        boolean createEvent,
                        int priority) {
        this.colorCode = colorCode;
        this.colorLabel = colorLabel;
        this.alarmLevel = alarmLevel;
        this.riskPointLevel = riskPointLevel;
        this.createAlarm = createAlarm;
        this.createEvent = createEvent;
        this.priority = priority;
    }

    public static AutoClosureSeverity classify(BigDecimal absoluteValue, IotProperties.Alarm.AutoClosure config) {
        if (absoluteValue == null) {
            return BLUE;
        }
        BigDecimal normalized = absoluteValue.abs();
        BigDecimal red = config == null || config.getRed() == null ? BigDecimal.valueOf(20) : config.getRed();
        BigDecimal orange = config == null || config.getOrange() == null ? BigDecimal.valueOf(10) : config.getOrange();
        BigDecimal yellow = config == null || config.getYellow() == null ? BigDecimal.valueOf(5) : config.getYellow();
        if (normalized.compareTo(red) >= 0) {
            return RED;
        }
        if (normalized.compareTo(orange) >= 0) {
            return ORANGE;
        }
        if (normalized.compareTo(yellow) >= 0) {
            return YELLOW;
        }
        return BLUE;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getColorLabel() {
        return colorLabel;
    }

    public String getAlarmLevel() {
        return alarmLevel;
    }

    public String getRiskPointLevel() {
        return riskPointLevel;
    }

    public boolean shouldCreateAlarm() {
        return createAlarm;
    }

    public boolean shouldCreateEvent() {
        return createEvent;
    }

    public int getPriority() {
        return priority;
    }
}
