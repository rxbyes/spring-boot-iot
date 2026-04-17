package com.ghlzm.iot.framework.protocol.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProtocolTemplateReplayDTO {

    @NotBlank(message = "templateCode 不能为空")
    private String templateCode;

    @NotBlank(message = "payloadJson 不能为空")
    private String payloadJson;
}
