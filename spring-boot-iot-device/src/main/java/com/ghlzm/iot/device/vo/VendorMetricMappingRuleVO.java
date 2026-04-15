package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 厂商字段映射规则读模型。
 */
@Data
public class VendorMetricMappingRuleVO {

    private Long id;

    private Long productId;

    private String scopeType;

    private String protocolCode;

    private String scenarioCode;

    private String deviceFamily;

    private String rawIdentifier;

    private String logicalChannelCode;

    private String relationConditionJson;

    private String normalizationRuleJson;

    private String targetNormativeIdentifier;

    private String status;

    private Integer versionNo;

    private String publishedStatus;

    private Integer publishedVersionNo;

    private Long approvalOrderId;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
