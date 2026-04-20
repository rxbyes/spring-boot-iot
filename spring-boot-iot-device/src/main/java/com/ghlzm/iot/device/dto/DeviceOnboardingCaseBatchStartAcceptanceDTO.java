package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 批量触发接入验收请求。
 */
@Data
public class DeviceOnboardingCaseBatchStartAcceptanceDTO {

    @NotEmpty(message = "caseIds 不能为空")
    private List<Long> caseIds;
}
