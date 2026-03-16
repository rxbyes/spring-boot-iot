package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 风险监测详情中的事件摘要。
 */
@Data
public class RiskMonitoringEventSummaryVO {

    private Long id;
    private String eventCode;
    private String eventTitle;
    private String riskLevel;
    private Integer status;
    private String currentValue;
    private String triggerTime;
}
