package com.ghlzm.iot.device.service.model;

import java.util.List;

/**
 * 接入案例验收运行进度。
 */
public record DeviceOnboardingAcceptanceProgress(String jobId,
                                                 String runId,
                                                 String status,
                                                 String summary,
                                                 List<String> failedLayers,
                                                 String jumpPath) {
}
