package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 采集器子设备明细返回对象。
 */
@Data
public class CollectorChildInsightChildVO {

    private String logicalChannelCode;
    private String childDeviceCode;
    private String childDeviceName;
    private String childProductKey;
    private String collectorLinkState;
    private String sensorStateValue;
    private LocalDateTime lastReportTime;
    private List<CollectorChildInsightMetricVO> metrics;
}
