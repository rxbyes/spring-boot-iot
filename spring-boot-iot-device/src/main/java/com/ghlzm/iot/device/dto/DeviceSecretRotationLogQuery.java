package com.ghlzm.iot.device.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 设备密钥轮换日志查询条件。
 */
@Data
public class DeviceSecretRotationLogQuery {

    private String deviceCode;

    private String productKey;

    private String rotationBatchId;

    private Long rotatedBy;

    private Long approvedBy;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;
}
