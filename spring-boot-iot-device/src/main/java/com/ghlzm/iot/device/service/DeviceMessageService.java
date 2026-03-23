package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.vo.DeviceMessageTraceStatsVO;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;

import java.util.List;

/**
 * 设备消息服务。
 */
public interface DeviceMessageService {

    /**
     * 根据设备编码查询最近的消息日志。
     */
    List<DeviceMessageLog> listMessageLogs(String deviceCode);

    /**
     * 按条件分页查询消息追踪日志。
     */
    PageResult<DeviceMessageLog> pageMessageTraceLogs(DeviceMessageTraceQuery query, Integer pageNum, Integer pageSize);

    /**
     * 查询消息追踪统计概览。
     */
    DeviceMessageTraceStatsVO getMessageTraceStats(DeviceMessageTraceQuery query);

    /**
     * 处理设备上行消息。
     */
    void handleUpMessage(DeviceUpMessage upMessage);

    void recordDispatchFailureTrace(String topic, byte[] payload, RawDeviceMessage rawDeviceMessage);
}
