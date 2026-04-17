package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 厂商字段映射规则批量状态变更请求。
 */
@Data
public class VendorMetricMappingRuleBatchStatusDTO {

    @NotEmpty(message = "ruleIds 不能为空")
    private List<Long> ruleIds;

    @NotBlank(message = "targetStatus 不能为空")
    private String targetStatus;
}
