package com.ghlzm.iot.device.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SensorStateHealth {
    REPORTED_NORMAL("reported_normal", "已上报-正常"),
    REPORTED_ABNORMAL("reported_abnormal", "已上报-异常"),
    MISSING("missing", "状态缺失"),
    STALE("stale", "状态过期");

    private final String code;
    private final String desc;
}
