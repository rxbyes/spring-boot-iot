package com.ghlzm.iot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 指令闭环最小状态枚举。
 */
@Getter
@AllArgsConstructor
public enum CommandStatusEnum {

    CREATED("CREATED", "已创建"),
    SENT("SENT", "已发送"),
    SUCCESS("SUCCESS", "执行成功"),
    FAILED("FAILED", "执行失败"),
    TIMEOUT("TIMEOUT", "执行超时");

    private final String code;
    private final String desc;
}
