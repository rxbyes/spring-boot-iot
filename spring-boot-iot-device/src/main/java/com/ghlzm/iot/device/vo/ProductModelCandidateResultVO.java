package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 产品物模型候选提炼结果。
 */
@Data
public class ProductModelCandidateResultVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    private ProductModelCandidateSummaryVO summary;

    private List<ProductModelCandidateVO> propertyCandidates = new ArrayList<>();

    private List<ProductModelCandidateVO> eventCandidates = new ArrayList<>();

    private List<ProductModelCandidateVO> serviceCandidates = new ArrayList<>();
}
