package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 产品物模型候选项。
 */
@Data
public class ProductModelCandidateVO {

    private String modelType;

    private String identifier;

    private String modelName;

    private String dataType;

    private String specsJson;

    private String eventType;

    private String serviceInputJson;

    private String serviceOutputJson;

    private Integer sortNo;

    private Integer requiredFlag;

    private String description;

    private String groupKey;

    private String evidenceOrigin;

    private String unit;

    private String normativeSource;

    private List<String> rawIdentifiers;

    private String monitorContentCode;

    private String monitorTypeCode;

    private String sensorCode;

    private Double confidence;

    private Boolean needsReview;

    private String candidateStatus;

    private String reviewReason;

    private Integer evidenceCount;

    private Integer messageEvidenceCount;

    private LocalDateTime lastReportTime;

    private List<String> sourceTables;

    private ProductModelProtocolTemplateEvidenceVO protocolTemplateEvidence;
}
