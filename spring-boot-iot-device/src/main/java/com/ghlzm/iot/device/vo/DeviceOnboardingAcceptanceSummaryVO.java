package com.ghlzm.iot.device.vo;

import java.util.List;
import lombok.Data;

/**
 * 接入案例验收摘要。
 */
@Data
public class DeviceOnboardingAcceptanceSummaryVO {

    private String jobId;

    private String runId;

    private String status;

    private String summary;

    private List<String> failedLayers;

    private String jumpPath;
}
