package com.ghlzm.iot.device.vo;

import lombok.Data;

/**
 * 厂商字段映射规则台账行。
 */
@Data
public class VendorMetricMappingRuleLedgerRowVO {

    private Long ruleId;

    private Long productId;

    private String rawIdentifier;

    private String targetNormativeIdentifier;

    private String scopeType;

    private String draftStatus;

    private Integer draftVersionNo;

    private String publishedStatus;

    private Integer publishedVersionNo;

    private Long latestApprovalOrderId;

    private String publishedSource;
}
