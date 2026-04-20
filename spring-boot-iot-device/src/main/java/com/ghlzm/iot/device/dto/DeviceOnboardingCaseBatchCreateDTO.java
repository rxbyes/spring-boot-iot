package com.ghlzm.iot.device.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 批量创建接入案例请求。
 */
@Data
public class DeviceOnboardingCaseBatchCreateDTO {

    @Valid
    @NotEmpty(message = "items 不能为空")
    private List<DeviceOnboardingCaseCreateDTO> items;
}
