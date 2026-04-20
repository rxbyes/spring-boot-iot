package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeviceOnboardingBatchActivateDTO {

    @NotEmpty(message = "请至少选择一条接入线索")
    private List<String> traceIds;

    @NotNull(message = "请确认接入建议")
    private Boolean confirmed;
}
