package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备接入失败归档统计概览。
 */
@Data
public class DeviceAccessErrorStatsVO {

    private Long total = 0L;

    private Long recentHourCount = 0L;

    private Long recent24HourCount = 0L;

    private Long distinctTraceCount = 0L;

    private Long distinctDeviceCount = 0L;

    private List<DeviceStatsBucketVO> topFailureStages = new ArrayList<>();

    private List<DeviceStatsBucketVO> topErrorCodes = new ArrayList<>();

    private List<DeviceStatsBucketVO> topExceptionClasses = new ArrayList<>();

    private List<DeviceStatsBucketVO> topProtocolCodes = new ArrayList<>();

    private List<DeviceStatsBucketVO> topTopics = new ArrayList<>();
}
