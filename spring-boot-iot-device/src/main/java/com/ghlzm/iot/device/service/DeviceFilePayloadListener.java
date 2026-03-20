package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;

/**
 * 文件/固件上行消息监听器扩展点。
 * 当前由 device 模块统一触发，后续 OTA 模块启用后可实现该接口接管更复杂的升级业务。
 */
public interface DeviceFilePayloadListener {

    /**
     * 处理已经完成基础持久化的文件/固件上行消息。
     */
    void onFilePayload(Device device, DeviceUpMessage upMessage);
}
