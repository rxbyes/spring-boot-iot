package com.ghlzm.iot.common.exception;

/**
 * 异常基类
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:35
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}

