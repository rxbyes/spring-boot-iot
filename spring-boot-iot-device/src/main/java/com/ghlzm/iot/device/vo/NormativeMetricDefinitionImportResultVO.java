package com.ghlzm.iot.device.vo;

import java.util.List;
import lombok.Data;

/**
 * 规范字段导入预检/执行结果。
 */
@Data
public class NormativeMetricDefinitionImportResultVO {

    private Integer totalCount;

    private Integer readyCount;

    private Integer conflictCount;

    private Integer appliedCount;

    private List<NormativeMetricDefinitionImportRowVO> rows;
}
