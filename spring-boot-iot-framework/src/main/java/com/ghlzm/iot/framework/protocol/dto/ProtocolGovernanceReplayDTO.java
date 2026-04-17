package com.ghlzm.iot.framework.protocol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolGovernanceReplayDTO {

    private String familyCode;

    private String protocolCode;

    private String appId;
}
