package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 产品物模型双证据治理 apply 结果。
 */
@Data
public class ProductModelGovernanceApplyResultVO {

    private Integer createdCount;

    private Integer updatedCount;

    private Integer skippedCount;

    private Integer conflictCount;

    private LocalDateTime lastAppliedAt;

    private List<ProductModelGovernanceAppliedItemVO> appliedItems;
}
