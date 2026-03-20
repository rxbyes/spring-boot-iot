package com.ghlzm.iot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 产品状态枚举。
 */
@Getter
@AllArgsConstructor
public enum ProductStatusEnum {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;
}
