package com.ghlzm.iot.common.exception;

/**
 * 异常基类
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:35
 */
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        this(500, message);
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
