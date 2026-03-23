package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息追踪统计概览。
 */
@Data
public class DeviceMessageTraceStatsVO {

    private Long total = 0L;

    private Long recentHourCount = 0L;

    private Long recent24HourCount = 0L;

    private Long distinctTraceCount = 0L;

    private Long distinctDeviceCount = 0L;

    private Long dispatchFailureCount = 0L;

    private List<DeviceStatsBucketVO> topMessageTypes = new ArrayList<>();

    private List<DeviceStatsBucketVO> topProductKeys = new ArrayList<>();

    private List<DeviceStatsBucketVO> topDeviceCodes = new ArrayList<>();

    private List<DeviceStatsBucketVO> topTopics = new ArrayList<>();
}
