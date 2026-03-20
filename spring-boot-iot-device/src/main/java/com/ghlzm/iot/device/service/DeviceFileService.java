package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.vo.DeviceFileSnapshotVO;
import com.ghlzm.iot.device.vo.DeviceFirmwareAggregateVO;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;

import java.util.List;

/**
 * 设备文件上行服务。
 * 当前负责最小文件流持久化、固件分片聚合和 OTA 扩展点转发。
 */
public interface DeviceFileService {

    /**
     * 处理 C.3 / C.4 文件类上行消息。
     */
    void handleFilePayload(Device device, DeviceUpMessage upMessage);

    /**
     * 查询设备最近的文件快照列表。
     */
    List<DeviceFileSnapshotVO> listFileSnapshots(String deviceCode);

    /**
     * 查询设备最近的固件分包聚合结果。
     */
    List<DeviceFirmwareAggregateVO> listFirmwareAggregates(String deviceCode);
}
