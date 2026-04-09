package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Contract release batch read model.
 */
@Data
public class ProductContractReleaseBatchVO {

    private Long id;

    private Long productId;

    private String scenarioCode;

    private String releaseSource;

    private Integer releasedFieldCount;

    private Long approvalOrderId;

    private String releaseReason;

    private String releaseStatus;

    private Long createBy;

    private LocalDateTime createTime;

    private Long rollbackBy;

    private LocalDateTime rollbackTime;
}
