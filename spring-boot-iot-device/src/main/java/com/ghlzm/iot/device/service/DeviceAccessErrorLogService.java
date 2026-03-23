package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.vo.DeviceAccessErrorStatsVO;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;

import java.util.Date;
import java.util.List;

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
     * 查询失败归档统计概览。
     */
    DeviceAccessErrorStatsVO getStats(DeviceAccessErrorQuery query);

    /**
     * 统计指定时间之后各 failureStage 的失败数量。
     */
    List<FailureStageCount> listFailureStageCountsSince(Date startTime);

    /**
     * 查询失败归档详情。
     */
    DeviceAccessErrorLog getById(Long id);

    record FailureStageCount(String failureStage, long failureCount) {
    }
}
