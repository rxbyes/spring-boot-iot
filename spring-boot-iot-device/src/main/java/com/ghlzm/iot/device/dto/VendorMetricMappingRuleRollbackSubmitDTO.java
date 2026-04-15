package com.ghlzm.iot.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 厂商字段映射规则回滚审批提交请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorMetricMappingRuleRollbackSubmitDTO {

    private String submitReason;
}
