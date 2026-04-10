package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Contract release batch impact analysis.
 */
@Data
public class ProductContractReleaseImpactVO {

    private Long batchId;

    private Long productId;

    private String scenarioCode;

    private String releaseSource;

    private Integer releasedFieldCount;

    private Integer totalBeforeCount;

    private Integer totalAfterCount;

    private Integer addedCount;

    private Integer removedCount;

    private Integer changedCount;

    private Integer unchangedCount;

    private LocalDateTime comparedAt;

    private List<ImpactItem> impactItems;

    private DependencySummary dependencySummary;

    @Data
    public static class ImpactItem {

        private String changeType;

        private String modelType;

        private String identifier;

        private List<String> changedFields;
    }

    @Data
    public static class DependencySummary {

        private Long affectedRiskMetricCount;

        private Long affectedRiskPointBindingCount;

        private Long affectedRuleCount;

        private Long affectedLinkageBindingCount;

        private Long affectedEmergencyPlanBindingCount;
    }
}
