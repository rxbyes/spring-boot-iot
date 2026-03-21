package com.ghlzm.iot.framework.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 站内消息内部发布命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppMessagePublishCommand {

    private Long tenantId;

    private String messageType;

    private String priority;

    private String title;

    private String summary;

    private String content;

    private String targetType;

    private List<String> targetRoleCodes;

    private List<Long> targetUserIds;

    private String relatedPath;

    private String sourceType;

    private String sourceId;

    private Date publishTime;

    private Date expireTime;

    private Integer status;

    private Integer sortNo;

    private Long operatorId;
}
