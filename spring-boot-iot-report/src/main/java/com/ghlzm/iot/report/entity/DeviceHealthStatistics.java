package com.ghlzm.iot.report.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * Device health statistics payload.
 */
@Data
public class DeviceHealthStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer totalCount;
    private Integer total;
    private Integer onlineCount;
    private Integer online;
    private Integer offlineCount;
    private Integer offline;
    private Double onlineRate;
    private Integer healthyCount;
    private Integer healthy;
    private Integer unhealthyCount;
    private Integer warning;
    private Integer critical;
}
