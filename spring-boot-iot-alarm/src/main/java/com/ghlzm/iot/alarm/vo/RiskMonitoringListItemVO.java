package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险监测实时列表项。
 */
@Data
public class RiskMonitoringListItemVO {

    private Long bindingId;
    private Long regionId;
    private String regionName;
    private Long riskPointId;
    private String riskPointName;
    private String riskLevel;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String productName;
    private String metricIdentifier;
    private String metricName;
    private String currentValue;
    private String unit;
    private String monitorStatus;
    private Integer onlineStatus;
    private LocalDateTime latestReportTime;
    private Boolean alarmFlag;
}
