package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 无代码接入模板包。
 */
@Data
@TableName("iot_onboarding_template_pack")
@EqualsAndHashCode(callSuper = true)
public class OnboardingTemplatePack extends BaseEntity {

    private String packCode;

    private String packName;

    private String scenarioCode;

    private String deviceFamily;

    private String status;

    private Integer versionNo;

    private String protocolFamilyCode;

    private String decryptProfileCode;

    private String protocolTemplateCode;

    private String defaultGovernanceConfigJson;

    private String defaultInsightConfigJson;

    private String defaultAcceptanceProfileJson;

    private String description;
}
