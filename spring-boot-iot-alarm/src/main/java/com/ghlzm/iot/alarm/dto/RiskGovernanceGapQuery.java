package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * 风险治理缺口查询条件。
 */
@Data
public class RiskGovernanceGapQuery {

    private String deviceCode;

    private Long riskPointId;

    private Long pageNum;

    private Long pageSize;
}
