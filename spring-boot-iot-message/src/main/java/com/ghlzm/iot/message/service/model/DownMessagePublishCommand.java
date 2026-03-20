package com.ghlzm.iot.message.service.model;

import lombok.Data;

import java.util.Map;

/**
 * 下行发布命令。
 */
@Data
public class DownMessagePublishCommand {

    private String protocolCode;
    private String productKey;
    private String deviceCode;
    private String topic;
    private Integer qos;
    private Boolean retained;
    private String messageId;
    private String commandType;
    private String serviceIdentifier;
    private Map<String, Object> params;
}
