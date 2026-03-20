package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 风险监测详情中的告警摘要。
 */
@Data
public class RiskMonitoringAlarmSummaryVO {

    private Long id;
    private String alarmCode;
    private String alarmTitle;
    private String alarmLevel;
    private Integer status;
    private String currentValue;
    private String thresholdValue;
    private String triggerTime;
}
