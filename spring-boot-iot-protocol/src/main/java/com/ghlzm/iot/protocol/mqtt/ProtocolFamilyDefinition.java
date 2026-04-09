package com.ghlzm.iot.protocol.mqtt;

import lombok.Data;

@Data
public class ProtocolFamilyDefinition {

    private String familyCode;
    private String protocolCode;
    private String displayName;
    private String decryptProfileCode;
    private String signAlgorithm;
    private String normalizationStrategy;
    private Boolean enabled;
}
