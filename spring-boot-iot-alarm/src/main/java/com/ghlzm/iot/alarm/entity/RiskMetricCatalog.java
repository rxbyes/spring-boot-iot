package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Risk metric catalog entity.
 */
@Data
@TableName("risk_metric_catalog")
public class RiskMetricCatalog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long productId;

    private Long productModelId;

    private String contractIdentifier;

    private String riskMetricCode;

    private String riskMetricName;

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

    private Integer deleted;
}
