package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * 风险点待治理绑定分页查询参数。
 */
@Data
public class RiskPointPendingBindingQuery {

    private Long riskPointId;

    private String deviceCode;

    private String resolutionStatus;

    private String batchNo;

    private Long pageNum;

    private Long pageSize;
}
