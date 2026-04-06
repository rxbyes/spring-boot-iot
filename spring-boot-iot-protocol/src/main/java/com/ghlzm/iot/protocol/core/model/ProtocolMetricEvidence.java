package com.ghlzm.iot.protocol.core.model;

import lombok.Data;

/**
 * 协议归一化过程中的指标证据。
 */
@Data
public class ProtocolMetricEvidence {

    private String rawIdentifier;

    private String canonicalIdentifier;

    private String logicalChannelCode;

    private String parentDeviceCode;

    private String childDeviceCode;

    private String sampleValue;

    private String valueType;

    private String evidenceOrigin;
}
