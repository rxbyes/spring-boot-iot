package com.ghlzm.iot.device.service.model;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import lombok.Data;

/**
 * 设备消息处理目标。
 */
@Data
public class DeviceProcessingTarget {

    private Device device;
    private Product product;
    private DeviceUpMessage message;
    private Boolean childTarget = Boolean.FALSE;
}
