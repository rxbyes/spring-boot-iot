package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommandRecordPageItemVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String commandId;

    private String deviceCode;

    private String productKey;

    private String topic;

    private String commandType;

    private String serviceIdentifier;

    private String status;

    private LocalDateTime sendTime;

    private LocalDateTime ackTime;

    private LocalDateTime timeoutTime;

    private String errorMessage;

    private String replyPayload;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
