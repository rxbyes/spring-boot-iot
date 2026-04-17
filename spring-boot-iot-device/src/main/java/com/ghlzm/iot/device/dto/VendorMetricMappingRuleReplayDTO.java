package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 厂商字段映射规则回放校验请求。
 */
@Data
public class VendorMetricMappingRuleReplayDTO {

    @NotBlank(message = "rawIdentifier 不能为空")
    private String rawIdentifier;

    private String logicalChannelCode;

    private String sampleValue;
}
