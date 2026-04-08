package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 厂商字段映射规则新增/编辑请求体。
 */
@Data
public class VendorMetricMappingRuleUpsertDTO {

    @NotBlank(message = "scopeType 不能为空")
    private String scopeType;

    private String protocolCode;

    private String scenarioCode;

    private String deviceFamily;

    @NotBlank(message = "rawIdentifier 不能为空")
    private String rawIdentifier;

    private String logicalChannelCode;

    private String relationConditionJson;

    private String normalizationRuleJson;

    @NotBlank(message = "targetNormativeIdentifier 不能为空")
    private String targetNormativeIdentifier;

    private String status;
}
