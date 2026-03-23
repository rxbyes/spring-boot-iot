package com.ghlzm.iot.device.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备侧统计桶摘要。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatsBucketVO {

    private String value;

    private String label;

    private Long count;
}
