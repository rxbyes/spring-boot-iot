package com.ghlzm.iot.device.dto;

import lombok.Data;

/**
 * 无代码接入案例查询参数。
 */
@Data
public class DeviceOnboardingCaseQueryDTO {

    private Long tenantId;

    private String keyword;

    private String status;

    private String currentStep;

    private Long pageNum;

    private Long pageSize;
}
