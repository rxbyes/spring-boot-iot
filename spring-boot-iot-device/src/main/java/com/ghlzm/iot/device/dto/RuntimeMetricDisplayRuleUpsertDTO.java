package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 运行态字段显示规则新增/编辑请求体。
 */
@Data
public class RuntimeMetricDisplayRuleUpsertDTO {

    @NotBlank(message = "scopeType 不能为空")
    private String scopeType;

    private String protocolCode;

    private String scenarioCode;

    private String deviceFamily;

    @NotBlank(message = "rawIdentifier 不能为空")
    private String rawIdentifier;

    @NotBlank(message = "displayName 不能为空")
    private String displayName;

    private String unit;

    private String status;
}
