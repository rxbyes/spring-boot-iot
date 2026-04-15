package com.ghlzm.iot.device.vo;

import lombok.Data;

/**
 * 厂商字段映射规则试命中结果。
 */
@Data
public class VendorMetricMappingRuleHitPreviewVO {

    private Boolean matched;

    private String hitSource;

    private Long ruleId;

    private String rawIdentifier;

    private String logicalChannelCode;

    private String targetNormativeIdentifier;

    private Integer publishedVersionNo;

    private Long approvalOrderId;
}
