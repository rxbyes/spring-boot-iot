package com.ghlzm.iot.device.vo;

import lombok.Data;

/**
 * 规范字段导入行预检结果。
 */
@Data
public class NormativeMetricDefinitionImportRowVO {

    private Integer rowIndex;

    private Long id;

    private String scenarioCode;

    private String deviceFamily;

    private String identifier;

    private String displayName;

    private String monitorContentCode;

    private String monitorTypeCode;

    private String fallbackKey;

    private String action;

    private String status;

    private String message;
}
