package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 产品物模型候选提炼摘要。
 */
@Data
public class ProductModelCandidateSummaryVO {

    private String extractionMode;

    private String sampleType;

    private String sampleDeviceCode;

    private String resolvedContractIdentifierMode;

    private Integer propertyEvidenceCount;

    private Integer propertyCandidateCount;

    private Integer eventEvidenceCount;

    private Integer eventCandidateCount;

    private Integer serviceEvidenceCount;

    private Integer serviceCandidateCount;

    private Integer needsReviewCount;

    private Integer existingModelCount;

    private Integer createdCount;

    private Integer skippedCount;

    private Integer conflictCount;

    private String eventHint;

    private String serviceHint;

    private Integer ignoredFieldCount;

    private LocalDateTime lastExtractedAt;
}
