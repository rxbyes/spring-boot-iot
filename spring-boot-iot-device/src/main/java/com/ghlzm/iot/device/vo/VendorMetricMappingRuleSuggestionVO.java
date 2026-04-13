package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 厂商字段映射规则建议读模型。
 */
@Data
public class VendorMetricMappingRuleSuggestionVO {

    private String rawIdentifier;

    private String logicalChannelCode;

    private String targetNormativeIdentifier;

    private String recommendedScopeType;

    private String status;

    private String confidence;

    private Integer evidenceCount;

    private String sampleValue;

    private String valueType;

    private String evidenceOrigin;

    private LocalDateTime lastSeenTime;

    private String reason;

    private Long existingRuleId;

    private String existingTargetNormativeIdentifier;
}
