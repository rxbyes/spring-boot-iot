package com.ghlzm.iot.device.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/4/22
 */
@Getter
@AllArgsConstructor
public enum DeviceTopologyRole {

    COLLECTOR_PARENT("collector-parent", "采集器父设备"),
    COLLECTOR_CHILD("collector-child", "采集器子设备"),
    STANDALONE("standalone", "单台直报设备");

    private final String code;
    private final String desc;
}
