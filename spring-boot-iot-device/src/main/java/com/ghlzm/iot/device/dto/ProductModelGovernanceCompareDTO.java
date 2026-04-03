package com.ghlzm.iot.device.dto;

import java.util.List;
import lombok.Data;

/**
 * 产品物模型双证据治理 compare 请求体。
 */
@Data
public class ProductModelGovernanceCompareDTO {

    private String governanceMode;

    private String normativePresetCode;

    private List<String> selectedNormativeIdentifiers;

    private ManualExtractInput manualExtract;

    private List<ManualDraftItem> manualDraftItems;

    private Boolean includeRuntimeCandidates;

    @Data
    public static class ManualExtractInput {

        private String sampleType;

        private String samplePayload;
    }

    @Data
    public static class ManualDraftItem {

        private String modelType;

        private String identifier;

        private String modelName;

        private String dataType;

        private String specsJson;

        private String eventType;

        private String serviceInputJson;

        private String serviceOutputJson;

        private String description;
    }
}
