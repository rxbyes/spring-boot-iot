package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险监测趋势点。
 */
@Data
public class RiskMonitoringTrendPointVO {

    private LocalDateTime reportTime;
    private String value;
    private Double numericValue;
}
