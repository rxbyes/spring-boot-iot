package com.ghlzm.iot.framework.advice;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:43
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException e) {
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() == null
                ? "请求参数不合法"
                : e.getBindingResult().getFieldError().getDefaultMessage();
        return R.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail("系统繁忙，请稍后再试");
    }
}
