package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.util.Date;

/**
 * 风险点绑定测点项。
 */
@Data
public class RiskPointBindingMetricVO {

    private Long bindingId;

    private String metricIdentifier;

    private String metricName;

    private String bindingSource;

    private Date createTime;
}
