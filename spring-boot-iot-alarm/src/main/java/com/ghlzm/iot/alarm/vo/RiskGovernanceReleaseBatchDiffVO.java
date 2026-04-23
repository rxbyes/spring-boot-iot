package com.ghlzm.iot.alarm.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Cross-batch release diff for contract fields and risk metric catalogs.
 */
@Data
public class RiskGovernanceReleaseBatchDiffVO {

    private Long productId;

    private BatchSummary baselineBatch;

    private BatchSummary targetBatch;

    private Integer baselineContractFieldCount;

    private Integer targetContractFieldCount;

    private Integer baselineMetricCount;

    private Integer targetMetricCount;

    private Integer addedContractCount;

    private Integer removedContractCount;

    private Integer changedContractCount;

    private Integer unchangedContractCount;

    private Integer addedMetricCount;

    private Integer removedMetricCount;

    private Integer changedMetricCount;

    private Integer unchangedMetricCount;

    private LocalDateTime comparedAt;

    private List<ContractDiffItem> contractDiffItems;

    private List<MetricDiffItem> metricDiffItems;

    @Data
    public static class BatchSummary {

        private Long id;

        private Long productId;

        private String scenarioCode;

        private String releaseSource;

        private Integer releasedFieldCount;

        private Long approvalOrderId;

        private String releaseReason;

        private String releaseStatus;

        private LocalDateTime createTime;

        private LocalDateTime rollbackTime;
    }

    @Data
    public static class ContractDiffItem {

        private String changeType;

        private String modelType;

        private String identifier;

        private List<String> changedFields;
    }

    @Data
    public static class MetricDiffItem {

        private String changeType;

        private String contractIdentifier;

        private String riskMetricCode;

        private String riskMetricName;

        private String metricRole;

        private String lifecycleStatus;

        private List<String> changedFields;
    }
}
