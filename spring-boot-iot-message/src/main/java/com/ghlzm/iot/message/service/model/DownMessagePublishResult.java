package com.ghlzm.iot.message.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 下行发布结果。
 */
@Data
@AllArgsConstructor
public class DownMessagePublishResult {

    private String protocolCode;
    private String topic;
    private Integer qos;
    private Boolean retained;
    private String deviceCode;
    private String productKey;
    private String commandType;
}
