package com.ghlzm.iot.device.service.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备链路状态刷新结果。
 */
@Data
public class DeviceStateRefreshResult {

    private String branch;
    private Map<String, Object> summary = new LinkedHashMap<>();
}
