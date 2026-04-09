package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Device secret rotation log entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device_secret_rotation_log")
public class DeviceSecretRotationLog extends BaseEntity {

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
