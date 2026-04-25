package com.ghlzm.iot.framework.observability.evidence;

/**
 * 可观测证据写入窄接口。
 * <p>
 * framework / message 等基础模块只依赖该接口，具体落库由 system 模块提供。
 */
public interface ObservabilityEvidenceRecorder {

    ObservabilityEvidenceRecorder NOOP = new ObservabilityEvidenceRecorder() {
    };

    default void recordSpan(ObservabilitySpanLogRecord span) {
        // no-op by default
    }

    default void recordBusinessEvent(BusinessEventLogRecord event) {
        // no-op by default
    }

    static ObservabilityEvidenceRecorder noop() {
        return NOOP;
    }
}
