package com.ghlzm.iot.report.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备健康统计实体
 */
@Data
public class DeviceHealthStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 设备总数
     */
    private Integer totalCount;

    /**
     * 在线设备数
     */
    private Integer onlineCount;

    /**
     * 离线设备数
     */
    private Integer offlineCount;

    /**
     * 在线率（百分比）
     */
    private Double onlineRate;

    /**
     * 健康设备数（最近24小时有上报）
     */
    private Integer healthyCount;

    /**
     * 不健康设备数
     */
    private Integer unhealthyCount;
}
