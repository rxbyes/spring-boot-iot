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

        private List<RiskMetricDetail> affectedRiskMetrics;

        private List<RiskPointBindingDetail> affectedRiskPointBindings;

        private List<RuleDetail> affectedRules;

        private List<LinkageBindingDetail> affectedLinkageBindings;

        private List<EmergencyPlanBindingDetail> affectedEmergencyPlanBindings;
    }

    @Data
    public static class RiskMetricDetail {

        private Long riskMetricId;

        private String contractIdentifier;

        private String normativeIdentifier;

        private String riskMetricCode;

        private String riskMetricName;

        private String metricRole;

        private String lifecycleStatus;
    }

    @Data
    public static class RiskPointBindingDetail {

        private Long bindingId;

        private Long riskPointId;

        private String riskPointName;

        private Long deviceId;

        private String deviceCode;

        private String deviceName;

        private Long riskMetricId;

        private String metricIdentifier;

        private String metricName;
    }

    @Data
    public static class RuleDetail {

        private Long ruleId;

        private String ruleName;

        private Long riskMetricId;

        private String metricIdentifier;

        private String metricName;

        private String alarmLevel;
    }

    @Data
    public static class LinkageBindingDetail {

        private Long bindingId;

        private Long linkageRuleId;

        private String linkageRuleName;

        private Long riskMetricId;

        private String bindingStatus;
    }

    @Data
    public static class EmergencyPlanBindingDetail {

        private Long bindingId;

        private Long emergencyPlanId;

        private String emergencyPlanName;

        private Long riskMetricId;

        private String bindingStatus;

        private String alarmLevel;
    }
}
