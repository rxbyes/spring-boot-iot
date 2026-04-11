package com.ghlzm.iot.device.vo;

import java.util.List;
import lombok.Data;

/**
 * 采集器子设备总览返回对象。
 */
@Data
public class CollectorChildInsightOverviewVO {

    private String parentDeviceCode;
    private Integer parentOnlineStatus;
    private Integer childCount;
    private Integer reachableChildCount;
    private Integer sensorStateReportedCount;
    private List<CollectorChildInsightChildVO> children;
}
