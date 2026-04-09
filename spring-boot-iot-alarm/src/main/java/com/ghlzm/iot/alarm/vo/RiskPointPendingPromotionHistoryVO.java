package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.util.Date;

/**
 * 待治理转正历史。
 */
@Data
public class RiskPointPendingPromotionHistoryVO {

    private Long id;

    private Long pendingBindingId;

    private Long riskPointDeviceId;

    private String metricIdentifier;

    private String metricName;

    private String promotionStatus;

    private String recommendationLevel;

    private Integer recommendationScore;

    private String promotionNote;

    private Long operatorId;

    private String operatorName;

    private Date createTime;
}
