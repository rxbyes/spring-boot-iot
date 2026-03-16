package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * GIS 风险点位聚合结果。
 */
@Data
public class RiskMonitoringGisPointVO {

    private Long regionId;
    private String regionName;
    private Long riskPointId;
    private String riskPointCode;
    private String riskPointName;
    private String riskLevel;
    private Double longitude;
    private Double latitude;
    private Integer deviceCount;
    private Integer onlineDeviceCount;
    private Integer activeAlarmCount;
}
