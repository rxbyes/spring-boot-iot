package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待治理测点候选。
 */
@Data
public class RiskPointPendingMetricCandidateVO {

    private Long riskMetricId;

    private String metricIdentifier;

    private String metricName;

    private String dataType;

    private List<String> evidenceSources;

    private LocalDateTime lastSeenTime;

    private String sampleValue;

    private Integer seenCount;

    private Integer recommendationScore;

    private String recommendationLevel;

    private Boolean catalogRecommended;

    private String reasonSummary;
}
