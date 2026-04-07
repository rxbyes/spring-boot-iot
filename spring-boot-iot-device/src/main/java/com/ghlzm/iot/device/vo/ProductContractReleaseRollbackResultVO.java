package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Contract release rollback result.
 */
@Data
public class ProductContractReleaseRollbackResultVO {

    private Long rolledBackBatchId;

    private Long productId;

    private String scenarioCode;

    private String releaseSource;

    private Integer releasedFieldCount;

    private Integer restoredFieldCount;

    private String rollbackMode;

    private String rollbackLimitations;

    private LocalDateTime rollbackTime;

    private Long approvalOrderId;
}
