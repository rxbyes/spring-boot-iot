package com.ghlzm.iot.device.dto;

import java.util.List;
import lombok.Data;

/**
 * 产品物模型契约字段 compare 请求体。
 */
@Data
public class ProductModelGovernanceCompareDTO {

    private ManualExtractInput manualExtract;

    @Data
    public static class ManualExtractInput {

        private String sampleType;

        private String deviceStructure;

        private String samplePayload;

        private String parentDeviceCode;

        private List<RelationMappingInput> relationMappings;
    }

    @Data
    public static class RelationMappingInput {

        private String logicalChannelCode;

        private String childDeviceCode;

        private String canonicalizationStrategy;

        private String statusMirrorStrategy;
    }
}
