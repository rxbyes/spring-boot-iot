package com.ghlzm.iot.device.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量新增设备请求体。
 */
@Data
public class DeviceBatchAddDTO {

    @Valid
    @NotEmpty(message = "请至少提供一条设备数据")
    private List<DeviceAddDTO> items;
}
