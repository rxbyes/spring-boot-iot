package com.ghlzm.iot.system.vo;

import lombok.Data;

@Data
public class InAppMessageBridgeAttemptVO {

    private Long id;

    private Long bridgeLogId;

    private Long messageId;

    private String channelCode;

    private String bridgeScene;

    private Integer attemptNo;

    private Integer bridgeStatus;

    private Integer unreadCount;

    private String recipientSnapshot;

    private Integer responseStatusCode;

    private String responseBody;

    private String attemptTime;
}
