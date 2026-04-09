package com.ghlzm.iot.device.dto;

import java.util.List;
import lombok.Data;

/**
 * 产品物模型双证据治理 apply 请求体。
 */
@Data
public class ProductModelGovernanceApplyDTO {

    private List<ApplyItem> items;

    @Data
    public static class ApplyItem {

        private String decision;

        private Long targetModelId;

        private String modelType;

        private String identifier;

        private String modelName;

        private String dataType;

        private String specsJson;

        private String eventType;

        private String serviceInputJson;

        private String serviceOutputJson;

        private Integer sortNo;

        private Integer requiredFlag;

        private String description;

        private String compareStatus;
    }
}
