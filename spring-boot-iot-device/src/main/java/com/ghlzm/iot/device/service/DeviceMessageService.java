package com.ghlzm.iot.device.service;

import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:32
 */
public interface DeviceMessageService {

    /**
     * 处理设备上行消息
     */
    void handleUpMessage(DeviceUpMessage upMessage);
}
