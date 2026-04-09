package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Device secret rotation result.
 */
@Data
public class DeviceSecretRotateResultVO {

    private Long deviceId;

    private String deviceCode;

    private String productKey;

    private String rotationBatchId;

    private Long rotatedBy;

    private Long approvedBy;

    private LocalDateTime rotatedAt;

    private String reason;
}
