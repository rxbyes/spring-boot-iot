package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 指令记录实体。
 * 当前只作为 Phase 3 Task 1 的最小持久化模型，不承载业务状态流转逻辑。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_command_record")
public class CommandRecord extends BaseEntity {

    private String commandId;
    private Long deviceId;
    private String deviceCode;
    private String productKey;
    private String gatewayDeviceCode;
    private String subDeviceCode;
    private String topic;
    private String commandType;
    private String serviceIdentifier;
    private String requestPayload;
    private String replyPayload;
    private Integer qos;
    private Integer retained;
    private String status;
    private LocalDateTime sendTime;
    private LocalDateTime ackTime;
    private LocalDateTime timeoutTime;
    private String errorMessage;
}
