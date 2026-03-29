package com.ghlzm.iot.device.dto;

import java.util.List;
import lombok.Data;

/**
 * 产品物模型候选确认入库请求。
 */
@Data
public class ProductModelCandidateConfirmDTO {

    private List<ProductModelCandidateConfirmItem> items;

    @Data
    public static class ProductModelCandidateConfirmItem {

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
    }
}
