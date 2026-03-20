package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;

/**
 * 设备接入失败归档服务。
 */
public interface DeviceAccessErrorLogService {

    /**
     * 归档 MQTT 接入失败原始报文。
     */
    void archiveMqttFailure(String topic,
                            byte[] payload,
                            RawDeviceMessage rawDeviceMessage,
                            String failureStage,
                            Throwable throwable);

    /**
     * 分页查询失败归档。
     */
    PageResult<DeviceAccessErrorLog> pageLogs(DeviceAccessErrorQuery query, Integer pageNum, Integer pageSize);

    /**
     * 查询失败归档详情。
     */
    DeviceAccessErrorLog getById(Long id);
}
