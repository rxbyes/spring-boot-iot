package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 无代码接入案例读模型。
 */
@Data
public class DeviceOnboardingCaseVO {

    private Long id;

    private Long tenantId;

    private String caseCode;

    private String caseName;

    private String scenarioCode;

    private String deviceFamily;

    private String protocolFamilyCode;

    private String decryptProfileCode;

    private String protocolTemplateCode;

    private Long templatePackId;

    private Long productId;

    private Long releaseBatchId;

    private String deviceCode;

    private String currentStep;

    private String status;

    private List<String> blockers;

    private DeviceOnboardingAcceptanceSummaryVO acceptance;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
