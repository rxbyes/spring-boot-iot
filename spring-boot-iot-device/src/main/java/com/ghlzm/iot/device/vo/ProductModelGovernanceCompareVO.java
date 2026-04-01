package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 产品物模型双证据治理 compare 结果。
 */
@Data
public class ProductModelGovernanceCompareVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;
}
