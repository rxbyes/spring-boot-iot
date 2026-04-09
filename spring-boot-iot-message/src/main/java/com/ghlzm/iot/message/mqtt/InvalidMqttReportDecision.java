package com.ghlzm.iot.message.mqtt;

/**
 * 无效 MQTT 上报治理决策。
 */
public record InvalidMqttReportDecision(
        boolean suppressed,
        boolean sampleFailure,
        InvalidMqttReportReason reason
) {

    public static InvalidMqttReportDecision allowSample(InvalidMqttReportReason reason) {
        return new InvalidMqttReportDecision(false, true, reason);
    }

    public static InvalidMqttReportDecision dropSuppressed() {
        return new InvalidMqttReportDecision(true, false, null);
    }

    public static InvalidMqttReportDecision dropSuppressed(InvalidMqttReportReason reason) {
        return new InvalidMqttReportDecision(true, false, reason);
    }
}
