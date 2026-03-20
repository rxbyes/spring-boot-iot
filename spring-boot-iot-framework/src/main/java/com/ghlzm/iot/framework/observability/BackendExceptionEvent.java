package com.ghlzm.iot.framework.observability;

import java.util.Collections;
import java.util.Map;

/**
 * 后台异常事件。
 * 供异步链路把运行时失败统一送入可检索的运维视图。
 */
public record BackendExceptionEvent(
        String operationModule,
        String operationMethod,
        String requestUrl,
        String requestMethod,
        Map<String, Object> context,
        Throwable throwable) {

    public BackendExceptionEvent {
        context = context == null ? Collections.emptyMap() : Collections.unmodifiableMap(context);
    }
}
