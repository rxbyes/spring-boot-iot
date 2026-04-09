package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.util.List;

/**
 * 风险点绑定按设备分组视图。
 */
@Data
public class RiskPointBindingDeviceGroupVO {

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Integer metricCount;

    private List<RiskPointBindingMetricVO> metrics;
}
