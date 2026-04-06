package com.ghlzm.iot.device.vo;

import lombok.Data;

/**
 * 设备测点选项，仅返回风险点绑定所需的最小字段。
 */
@Data
public class DeviceMetricOptionVO {

    private String identifier;
    private String name;
    private String dataType;
    private Long riskMetricId;
}
