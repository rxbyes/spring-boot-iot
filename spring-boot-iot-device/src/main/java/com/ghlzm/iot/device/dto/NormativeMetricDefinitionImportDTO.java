package com.ghlzm.iot.device.dto;

import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;

/**
 * 规范字段导入请求。
 */
@Data
public class NormativeMetricDefinitionImportDTO {

    @Valid
    private List<NormativeMetricDefinitionImportItemDTO> items;
}
