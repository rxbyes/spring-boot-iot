package com.ghlzm.iot.framework.observability;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 对最终日志行做统一脱敏，避免普通日志输出敏感信息。
 */
public class SensitiveDataMaskingPatternLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        return SensitiveLogSanitizer.sanitize(super.doLayout(event));
    }
}
