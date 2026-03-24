package com.ghlzm.iot.system.vo;

import lombok.Data;

@Data
public class InAppMessageBridgeLogVO {

    private Long id;

    private Long messageId;

    private String title;

    private String messageType;

    private String priority;

    private String sourceType;

    private String sourceId;

    private String relatedPath;

    private String publishTime;

    private String channelCode;

    private String channelName;

    private String channelType;

    private String bridgeScene;

    private Integer bridgeStatus;

    private Integer unreadCount;

    private Integer attemptCount;

    private String lastAttemptTime;

    private String successTime;

    private Integer responseStatusCode;

    private String responseBody;
}
