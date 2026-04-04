package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * 风险点单测点替换请求。
 */
@Data
public class RiskPointBindingReplaceRequest {

    private String metricIdentifier;

    private String metricName;
}
