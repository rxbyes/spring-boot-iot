package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 设备关系新增/更新请求。
 */
@Data
public class DeviceRelationUpsertDTO {

    @NotBlank(message = "父设备编码不能为空")
    private String parentDeviceCode;

    @NotBlank(message = "逻辑通道编码不能为空")
    private String logicalChannelCode;

    @NotBlank(message = "子设备编码不能为空")
    private String childDeviceCode;

    @NotBlank(message = "关系类型不能为空")
    private String relationType;

    @NotBlank(message = "归一化策略不能为空")
    private String canonicalizationStrategy;

    private String statusMirrorStrategy;

    private Integer enabled;

    private String remark;
}
