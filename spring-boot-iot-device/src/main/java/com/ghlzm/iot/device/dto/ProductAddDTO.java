package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:59
 */
@Data
public class ProductAddDTO {

    @NotBlank
    private String productKey;

    @NotBlank
    private String productName;

    @NotBlank
    private String protocolCode;

    @NotNull
    private Integer nodeType;

    private String dataFormat;

    private String manufacturer;

    private String description;

    private Integer status;
}

