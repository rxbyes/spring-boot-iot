package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 风险治理运维告警条目。
 */
@Data
public class RiskGovernanceOpsAlertItemVO {

    private String alertType;

    private String alertLabel;

    private Long productId;

    private String productKey;

    private String productName;

    private Long affectedCount;

    private String sampleIdentifier;

    private String sampleDetail;
}
