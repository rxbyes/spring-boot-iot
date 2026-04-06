package com.ghlzm.iot.protocol.core.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 上行协议解码阶段输出的补充元数据。
 * 当前主要服务于 `$dp` 家族治理和 message-flow 可观测增强。
 */
@Data
public class DeviceUpProtocolMetadata {

    private String appId;
    private List<String> familyCodes;
    private String normalizationStrategy;
    private String timestampSource;
    private Boolean childSplitApplied;
    private String routeType;
    private String decryptedPayloadPreview;
    private Map<String, Object> decodedPayloadPreview;
    private ProtocolTemplateEvidence templateEvidence;
    private List<ProtocolMetricEvidence> metricEvidence;
}
