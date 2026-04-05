package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 产品物模型手动提炼请求。
 */
@Data
public class ProductModelManualExtractDTO {

    @NotBlank(message = "请选择样本类型")
    private String sampleType;

    @NotBlank(message = "请输入样本报文")
    private String samplePayload;

    private String sourceDeviceCode;

    private String extractMode;
}
