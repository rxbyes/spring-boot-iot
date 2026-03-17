package com.ghlzm.iot.framework.observability;

/**
 * 后台异常记录器。
 */
public interface BackendExceptionRecorder {

    void record(BackendExceptionEvent event);
}
