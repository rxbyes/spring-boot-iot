package com.ghlzm.iot.system.vo;

import lombok.Data;

import java.util.Date;

@Data
public class InAppMessageAccessVO {

    private Long id;

    private String messageType;

    private String priority;

    private String title;

    private String summary;

    private String content;

    private String targetType;

    private String relatedPath;

    private String sourceType;

    private String sourceId;

    private Date publishTime;

    private Date expireTime;

    private Boolean read;

    private Date readTime;
}
