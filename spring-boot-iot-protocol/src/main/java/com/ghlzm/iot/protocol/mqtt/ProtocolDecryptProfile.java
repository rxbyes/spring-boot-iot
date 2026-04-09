package com.ghlzm.iot.protocol.mqtt;

import lombok.Data;

@Data
public class ProtocolDecryptProfile {

    private String profileCode;
    private String algorithm;
    private String merchantSource;
    private String merchantKey;
    private String transformation;
    private String signatureSecret;
    private Boolean enabled;
}
