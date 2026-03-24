package com.ghlzm.iot.device.service.model;

import lombok.Data;

/**
 * 遥测指标 legacy 映射模型。
 */
@Data
public class TelemetryMetricMapping {

    private String metricCode;
    private Boolean enabled = Boolean.TRUE;
    private String stable;
    private String column;
    private String source;
    private String reason;

    public boolean isLegacyUsable() {
        return Boolean.TRUE.equals(enabled)
                && hasText(stable)
                && hasText(column)
                && !hasText(reason);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
