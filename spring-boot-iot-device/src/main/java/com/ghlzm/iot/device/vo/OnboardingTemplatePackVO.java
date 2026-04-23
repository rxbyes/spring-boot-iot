package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 无代码接入模板包视图。
 */
@Data
public class OnboardingTemplatePackVO {

    private Long id;

    private Long tenantId;

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

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
