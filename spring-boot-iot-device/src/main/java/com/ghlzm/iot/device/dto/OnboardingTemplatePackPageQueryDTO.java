package com.ghlzm.iot.device.dto;

import lombok.Data;

/**
 * 无代码接入模板包分页查询。
 */
@Data
public class OnboardingTemplatePackPageQueryDTO {

    private Long tenantId;

    private String keyword;

    private String status;

    private String scenarioCode;

    private String deviceFamily;

    private Long pageNum;

    private Long pageSize;
}
