package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 无代码接入案例创建请求。
 */
@Data
public class DeviceOnboardingCaseCreateDTO {

    private Long tenantId;

    @NotBlank(message = "caseCode 不能为空")
    private String caseCode;

    @NotBlank(message = "caseName 不能为空")
    private String caseName;

    private String scenarioCode;

    private String deviceFamily;

    private String protocolFamilyCode;

    private String decryptProfileCode;

    private String protocolTemplateCode;

    private Long templatePackId;

    private Long productId;

    private Long releaseBatchId;

    private String deviceCode;

    private String remark;
}
