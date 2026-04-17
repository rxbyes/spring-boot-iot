package com.ghlzm.iot.device.vo;

import lombok.Data;

/**
 * 厂商字段映射规则回放校验结果。
 */
@Data
public class VendorMetricMappingRuleReplayVO {

    private Boolean matched;

    private String hitSource;

    private String matchedScopeType;

    private Long ruleId;

    private String rawIdentifier;

    private String logicalChannelCode;

    private String targetNormativeIdentifier;

    private String canonicalIdentifier;

    private String sampleValue;
}
