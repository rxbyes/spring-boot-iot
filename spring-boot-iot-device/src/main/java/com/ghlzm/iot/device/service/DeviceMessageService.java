package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.vo.DeviceMessageTraceStatsVO;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
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
    default List<DeviceMessageLog> listMessageLogs(String deviceCode) {
        return listMessageLogs(null, deviceCode);
    }

    List<DeviceMessageLog> listMessageLogs(Long currentUserId, String deviceCode);

    /**
     * 按条件分页查询消息追踪日志。
     */
    default PageResult<DeviceMessageLog> pageMessageTraceLogs(DeviceMessageTraceQuery query, Integer pageNum, Integer pageSize) {
        return pageMessageTraceLogs(null, query, pageNum, pageSize);
    }

    PageResult<DeviceMessageLog> pageMessageTraceLogs(Long currentUserId, DeviceMessageTraceQuery query, Integer pageNum, Integer pageSize);

    /**
     * 查询消息追踪统计概览。
     */
    default DeviceMessageTraceStatsVO getMessageTraceStats(DeviceMessageTraceQuery query) {
        return getMessageTraceStats(null, query);
    }

    DeviceMessageTraceStatsVO getMessageTraceStats(Long currentUserId, DeviceMessageTraceQuery query);

    /**
     * 查询链路追踪详情。
     */
    default MessageTraceDetailVO getMessageTraceDetail(Long id) {
        return getMessageTraceDetail(null, id);
    }

    MessageTraceDetailVO getMessageTraceDetail(Long currentUserId, Long id);

    /**
     * 处理设备上行消息。
     */
    void handleUpMessage(DeviceUpMessage upMessage);

    void recordDispatchFailureTrace(String topic, byte[] payload, RawDeviceMessage rawDeviceMessage);
}
