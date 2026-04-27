package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险治理缺口项。
 */
@Data
public class RiskGovernanceGapItemVO {

    private String issueType;

    private String issueLabel;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Long productId;

    private String productKey;

    private String productName;

    private Long riskPointId;

    private String riskPointName;

    private Long riskMetricId;

    private String metricIdentifier;

    private String metricName;

    private LocalDateTime lastReportTime;
}
