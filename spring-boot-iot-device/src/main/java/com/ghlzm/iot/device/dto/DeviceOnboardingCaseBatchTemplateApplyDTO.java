package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 批量套用模板包请求。
 */
@Data
public class DeviceOnboardingCaseBatchTemplateApplyDTO {

    @NotEmpty(message = "caseIds 不能为空")
    private List<Long> caseIds;

    @NotNull(message = "templatePackId 不能为空")
    private Long templatePackId;
}
