package com.ghlzm.iot.device.dto;

import lombok.Data;

/**
 * message-flow 最近会话查询条件。
 */
@Data
public class MessageFlowRecentQuery {

    private String deviceCode;
    private String topic;
    private String transportMode;
    private String status;
}
