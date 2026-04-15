package com.ghlzm.iot.framework.protocol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolFamilyDefinitionUpsertDTO {

    @NotBlank(message = "familyCode 不能为空")
    private String familyCode;

    @NotBlank(message = "protocolCode 不能为空")
    private String protocolCode;

    @NotBlank(message = "displayName 不能为空")
    private String displayName;

    private String decryptProfileCode;

    private String signAlgorithm;

    private String normalizationStrategy;
}
