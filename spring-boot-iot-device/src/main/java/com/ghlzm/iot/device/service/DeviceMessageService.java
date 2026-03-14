package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;

import java.util.List;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:32
 */
public interface DeviceMessageService {

    /**
     * 根据设备编码查询最近的消息日志。
     */
    List<DeviceMessageLog> listMessageLogs(String deviceCode);

    /**
     * 处理设备上行消息。
     * 当前方法保留给后续上报链路使用，本轮不扩展逻辑。
     */
    void handleUpMessage(DeviceUpMessage upMessage);
}
