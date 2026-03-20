package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 产品关联设备统计聚合结果。
 */
@Data
public class ProductDeviceStatRow {

    private Long productId;

    private Long deviceCount;

    private Long onlineDeviceCount;

    private LocalDateTime lastReportTime;
}
