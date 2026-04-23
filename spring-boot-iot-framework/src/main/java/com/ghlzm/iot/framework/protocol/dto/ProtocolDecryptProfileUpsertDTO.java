package com.ghlzm.iot.framework.protocol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolDecryptProfileUpsertDTO {

    @NotBlank(message = "profileCode 不能为空")
    private String profileCode;

    @NotBlank(message = "algorithm 不能为空")
    private String algorithm;

    @NotBlank(message = "merchantSource 不能为空")
    private String merchantSource;

    @NotBlank(message = "merchantKey 不能为空")
    private String merchantKey;

    private String transformation;

    private String signatureSecret;
}
