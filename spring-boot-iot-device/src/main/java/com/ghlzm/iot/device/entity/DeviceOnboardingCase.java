package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 无代码接入案例编排对象。
 */
@Data
@TableName("iot_device_onboarding_case")
@EqualsAndHashCode(callSuper = true)
public class DeviceOnboardingCase extends BaseEntity {

    private String caseCode;

    private String caseName;

    private String scenarioCode;

    private String deviceFamily;

    private String protocolFamilyCode;

    private String decryptProfileCode;

    private String protocolTemplateCode;

    private Long productId;

    private Long releaseBatchId;

    private String currentStep;

    private String status;

    private String blockerSummaryJson;

    private String evidenceSummaryJson;
}
