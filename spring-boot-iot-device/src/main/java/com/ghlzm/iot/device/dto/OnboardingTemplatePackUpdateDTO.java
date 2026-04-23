package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 无代码接入模板包更新请求。
 */
@Data
public class OnboardingTemplatePackUpdateDTO {

    private Long tenantId;

    @NotBlank(message = "packCode 不能为空")
    private String packCode;

    @NotBlank(message = "packName 不能为空")
    private String packName;

    private String scenarioCode;

    private String deviceFamily;

    private String status;

    private String protocolFamilyCode;

    private String decryptProfileCode;

    private String protocolTemplateCode;

    private String defaultGovernanceConfigJson;

    private String defaultInsightConfigJson;

    private String defaultAcceptanceProfileJson;

    private String description;

    private String remark;
}
