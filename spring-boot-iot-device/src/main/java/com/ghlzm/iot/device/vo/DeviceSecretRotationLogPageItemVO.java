package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 设备密钥轮换日志分页项。
 */
@Data
public class DeviceSecretRotationLogPageItemVO {

    private Long id;

    private Long deviceId;

    private String deviceCode;

    private String productKey;

    private String rotationBatchId;

    private String reason;

    private String previousSecretDigest;

    private String currentSecretDigest;

    private Long rotatedBy;

    private Long approvedBy;

    private LocalDateTime rotateTime;
}
