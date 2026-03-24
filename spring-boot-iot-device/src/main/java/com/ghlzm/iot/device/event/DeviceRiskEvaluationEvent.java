package com.ghlzm.iot.device.event;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备属性落库完成后的风险判定事件。
 */
public class DeviceRiskEvaluationEvent {

    private final Long tenantId;
    private final Long deviceId;
    private final String deviceCode;
    private final String deviceName;
    private final Long productId;
    private final String productKey;
    private final String protocolCode;
    private final String messageType;
    private final String topic;
    private final String traceId;
    private final LocalDateTime reportedAt;
    private final Map<String, Object> properties;

    public DeviceRiskEvaluationEvent(Long tenantId,
                                     Long deviceId,
                                     String deviceCode,
                                     String deviceName,
                                     Long productId,
                                     String productKey,
                                     String protocolCode,
                                     String messageType,
                                     String topic,
                                     String traceId,
                                     LocalDateTime reportedAt,
                                     Map<String, Object> properties) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.deviceCode = deviceCode;
        this.deviceName = deviceName;
        this.productId = productId;
        this.productKey = productKey;
        this.protocolCode = protocolCode;
        this.messageType = messageType;
        this.topic = topic;
        this.traceId = traceId;
        this.reportedAt = reportedAt;
        this.properties = properties;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductKey() {
        return productKey;
    }

    public String getProtocolCode() {
        return protocolCode;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getTopic() {
        return topic;
    }

    public String getTraceId() {
        return traceId;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
