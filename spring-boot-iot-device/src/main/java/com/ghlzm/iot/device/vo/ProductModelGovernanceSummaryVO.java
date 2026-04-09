package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 物模型治理摘要。
 */
@Data
public class ProductModelGovernanceSummaryVO {

    private Integer manualCount;

    private Integer runtimeCount;

    private Integer formalCount;

    private Integer propertyCount;

    private Integer eventCount;

    private Integer serviceCount;

    private Integer doubleAlignedCount;

    private Integer manualOnlyCount;

    private Integer runtimeOnlyCount;

    private Integer formalExistsCount;

    private Integer suspectedConflictCount;

    private Integer evidenceInsufficientCount;

    private LocalDateTime lastComparedAt;
}
