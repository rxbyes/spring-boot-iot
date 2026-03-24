package com.ghlzm.iot.protocol.core.model;

import lombok.Data;

import java.util.List;

@Data
public class DeviceUpProtocolMetadata {

    private String appId;
    private List<String> familyCodes;
    private String normalizationStrategy;
    private String timestampSource;
    private Boolean childSplitApplied;
    private String routeType;
}
