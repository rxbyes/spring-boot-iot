package com.ghlzm.iot.framework.protocol.vo;

import lombok.Data;

@Data
public class ProtocolDecryptPreviewVO {

    private Boolean matched;
    private String hitSource;
    private String familyCode;
    private String resolvedProfileCode;
    private String algorithm;
    private String merchantSource;
    private String merchantKey;
    private String transformation;
}
