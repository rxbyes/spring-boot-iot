package com.ghlzm.iot.system.vo;

import lombok.Data;

/**
 * 站内消息桥接失败聚合桶。
 */
@Data
public class InAppMessageBridgeFailureCountVO {

    private String channelCode;

    private String channelName;

    private Long failureCount = 0L;
}
