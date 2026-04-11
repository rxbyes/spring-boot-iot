package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 采集器子设备监测指标返回对象。
 */
@Data
public class CollectorChildInsightMetricVO {

    private String identifier;
    private String displayName;
    private String propertyValue;
    private String unit;
    private LocalDateTime reportTime;
}
