package com.ghlzm.iot.alarm.dto;

import lombok.Data;

import java.util.List;

/**
 * 风险点正式绑定批量请求。
 */
@Data
public class RiskPointBatchBindDeviceRequest {

    private Long riskPointId;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private List<RiskPointBindMetricDTO> metrics;
}
