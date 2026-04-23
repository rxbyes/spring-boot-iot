package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceOnboardingSuggestionQuery {

    @NotBlank(message = "traceId 不能为空")
    private String traceId;
}
