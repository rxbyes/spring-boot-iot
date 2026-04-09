package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.util.Date;

/**
 * 风险点待治理绑定分页项。
 */
@Data
public class RiskPointPendingBindingItemVO {

    private Long id;

    private String batchNo;

    private String sourceFileName;

    private Integer sourceRowNo;

    private Long riskPointId;

    private String riskPointCode;

    private String riskPointName;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private String resolutionStatus;

    private String resolutionNote;

    private String metricIdentifier;

    private String metricName;

    private Long promotedBindingId;

    private Date promotedTime;

    private Date createTime;
}
