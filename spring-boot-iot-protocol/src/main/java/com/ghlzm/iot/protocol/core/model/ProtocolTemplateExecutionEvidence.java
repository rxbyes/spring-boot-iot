package com.ghlzm.iot.protocol.core.model;

import lombok.Data;

import java.util.List;

/**
 * 协议模板执行证据。
 */
@Data
public class ProtocolTemplateExecutionEvidence {

    private String templateCode;
    private String logicalChannelCode;
    private String childDeviceCode;
    private String canonicalizationStrategy;
    private Boolean statusMirrorApplied;
    private List<String> parentRemovalKeys = List.of();
}
