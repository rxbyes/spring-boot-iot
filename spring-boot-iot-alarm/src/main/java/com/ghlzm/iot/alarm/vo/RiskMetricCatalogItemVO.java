package com.ghlzm.iot.alarm.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Risk metric catalog read model.
 */
@Data
public class RiskMetricCatalogItemVO {

    private Long id;

    private Long productId;

    private Long releaseBatchId;

    private Long productModelId;

    private String normativeIdentifier;

    private String contractIdentifier;

    private String riskMetricCode;

    private String riskMetricName;

    private String riskCategory;

    private String metricRole;

    private String lifecycleStatus;

    private String sourceScenarioCode;

    private String metricUnit;

    private String metricDimension;

    private String thresholdType;

    private String semanticDirection;

    private String thresholdDirection;

    private Integer trendEnabled;

    private Integer gisEnabled;

    private Integer insightEnabled;

    private Integer analyticsEnabled;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
