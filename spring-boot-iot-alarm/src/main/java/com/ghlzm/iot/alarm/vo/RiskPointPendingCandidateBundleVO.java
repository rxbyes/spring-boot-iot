package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 待治理候选结果集合。
 */
@Data
public class RiskPointPendingCandidateBundleVO {

    private Long pendingId;

    private String batchNo;

    private Long riskPointId;

    private String riskPointCode;

    private String riskPointName;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private String resolutionStatus;

    private String resolutionNote;

    private String metricIdentifier;

    private String metricName;

    private Date createTime;

    private List<RiskPointPendingMetricCandidateVO> candidates;

    private List<RiskPointPendingPromotionHistoryVO> promotionHistory;
}
