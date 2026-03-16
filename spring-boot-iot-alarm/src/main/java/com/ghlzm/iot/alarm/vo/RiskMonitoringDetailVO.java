package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险监测详情。
 */
@Data
public class RiskMonitoringDetailVO {

    private Long bindingId;
    private Long regionId;
    private String regionName;
    private Long riskPointId;
    private String riskPointCode;
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
    private String valueType;
    private String monitorStatus;
    private Integer onlineStatus;
    private LocalDateTime latestReportTime;
    private Object longitude;
    private Object latitude;
    private String address;
    private Long activeAlarmCount;
    private Long recentEventCount;
    private List<RiskMonitoringTrendPointVO> trendPoints;
    private List<RiskMonitoringAlarmSummaryVO> recentAlarms;
    private List<RiskMonitoringEventSummaryVO> recentEvents;
}
