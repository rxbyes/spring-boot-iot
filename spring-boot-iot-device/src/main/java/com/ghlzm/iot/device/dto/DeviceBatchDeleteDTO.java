package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除设备请求体。
 */
@Data
public class DeviceBatchDeleteDTO {

    @NotEmpty(message = "请选择需要删除的设备")
    private List<Long> ids;
}
