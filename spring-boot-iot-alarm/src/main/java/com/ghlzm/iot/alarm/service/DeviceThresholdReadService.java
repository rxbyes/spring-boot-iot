package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.vo.DeviceThresholdOverviewVO;

public interface DeviceThresholdReadService {

    DeviceThresholdOverviewVO getDeviceThresholds(Long currentUserId, Long deviceId);
}
