package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Device secret rotation request.
 */
@Data
public class DeviceSecretRotateDTO {

    @NotBlank(message = "newDeviceSecret 不能为空")
    private String newDeviceSecret;

    private String reason;
}
