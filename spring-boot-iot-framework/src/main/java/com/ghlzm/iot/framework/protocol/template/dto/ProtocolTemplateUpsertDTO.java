package com.ghlzm.iot.framework.protocol.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProtocolTemplateUpsertDTO {

    @NotBlank(message = "templateCode 不能为空")
    private String templateCode;

    @NotBlank(message = "familyCode 不能为空")
    private String familyCode;

    @NotBlank(message = "protocolCode 不能为空")
    private String protocolCode;

    @NotBlank(message = "displayName 不能为空")
    private String displayName;

    @NotBlank(message = "expressionJson 不能为空")
    private String expressionJson;

    private String outputMappingJson;
}
