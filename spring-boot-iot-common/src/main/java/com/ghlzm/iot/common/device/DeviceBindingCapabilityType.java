package com.ghlzm.iot.common.device;

/**
 * 风险绑定时使用的设备能力类型。
 */
public enum DeviceBindingCapabilityType {

    MONITORING,
    WARNING,
    VIDEO,
    UNKNOWN;

    public boolean supportsMetricBinding() {
        return this == MONITORING;
    }
}
