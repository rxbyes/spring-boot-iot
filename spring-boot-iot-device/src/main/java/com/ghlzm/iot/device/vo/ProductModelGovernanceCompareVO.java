package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 产品物模型双证据治理 compare 结果。
 */
@Data
public class ProductModelGovernanceCompareVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    private ProductModelGovernanceSummaryVO summary;

    private ProductModelCandidateSummaryVO manualSummary;

    private ProductModelCandidateSummaryVO runtimeSummary;

    private ProductModelGovernanceSummaryVO formalSummary;

    private List<ProductModelGovernanceCompareRowVO> compareRows = new ArrayList<>();
}
