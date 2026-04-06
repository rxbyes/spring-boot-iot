package com.ghlzm.iot.alarm.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 风险指标目录读模型。
 */
@Data
public class RiskMetricCatalogItemVO {

    private Long id;

    private Long productId;

    private Long productModelId;

    private String contractIdentifier;

    private String riskMetricCode;

    private String riskMetricName;

    private String thresholdDirection;

    private Integer trendEnabled;

    private Integer gisEnabled;

    private Integer insightEnabled;

    private Integer analyticsEnabled;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
