package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * 风险监测实时列表查询参数。
 */
@Data
public class RiskMonitoringListQuery {

    private Long regionId;
    private Long riskPointId;
    private String deviceCode;
    private String currentRiskLevel;
    private String riskLevel;
    private Integer onlineStatus;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
