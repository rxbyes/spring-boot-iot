package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 契约发布回滚结果。
 */
@Data
public class ProductContractReleaseRollbackResultVO {

    private Long rolledBackBatchId;

    private Long productId;

    private String scenarioCode;

    private String releaseSource;

    private Integer releasedFieldCount;

    private String rollbackMode;

    private String rollbackLimitations;

    private LocalDateTime rollbackTime;
}
