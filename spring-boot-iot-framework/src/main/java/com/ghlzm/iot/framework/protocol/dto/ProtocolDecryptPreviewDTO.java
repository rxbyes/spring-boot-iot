package com.ghlzm.iot.framework.protocol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolDecryptPreviewDTO {

    private String appId;

    private String protocolCode;

    private String familyCode;
}
