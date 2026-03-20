package com.ghlzm.iot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:16
 */
@Getter
@AllArgsConstructor
public enum DeviceStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;
}

