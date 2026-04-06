package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 契约发布批次读模型。
 */
@Data
public class ProductContractReleaseBatchVO {

    private Long id;

    private Long productId;

    private String scenarioCode;

    private String releaseSource;

    private Integer releasedFieldCount;

    private Long createBy;

    private LocalDateTime createTime;
}
