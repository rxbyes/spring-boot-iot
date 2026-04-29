package com.ghlzm.iot.device.dto;

import lombok.Data;

/**
 * 规范字段导入条目。
 */
@Data
public class NormativeMetricDefinitionImportItemDTO {

    private Long id;

    private String scenarioCode;

    private String deviceFamily;

    private String identifier;

    private String displayName;

    private String unit;

    private Integer precisionDigits;

    private String monitorContentCode;

    private String monitorTypeCode;

    private Integer riskEnabled;

    private Integer trendEnabled;

    private String metricDimension;

    private String thresholdType;

    private String semanticDirection;

    private Integer gisEnabled;

    private Integer insightEnabled;

    private Integer analyticsEnabled;

    private String status;

    private Integer versionNo;

    private Object metadataJson;
}
